/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak.core.monitor
import akka.actor.Actor
import scala.collection.mutable.ListBuffer
import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap
import akka.setak.core.TestMessageEnvelop
import akka.setak.core.RealMessageEnvelop
import akka.setak.core.MessageEventEnum._
import akka.actor.LocalActorRef
import akka.actor.UntypedChannel

abstract class MonitorActorMessage
case class AsyncMessageEvent(message: RealMessageEnvelop, event: MessageEventType) extends MonitorActorMessage
case class ReplyMessageEvent(message: RealMessageEnvelop) extends MonitorActorMessage
case class MatchedMessageEventCount(testMessage: TestMessageEnvelop, event: MessageEventType) extends MonitorActorMessage
case class AddTestMessageEnvelop(testMessage: TestMessageEnvelop) extends MonitorActorMessage
case object AllDeliveredMessagesAreProcessed extends MonitorActorMessage
case object AllTestMessagesAreProcessed extends MonitorActorMessage
case class NotifyMeForMessageEvent(testMessages: Set[TestMessageEnvelop], event: MessageEventType) extends MonitorActorMessage
case object NotProcessedMessages extends MonitorActorMessage
case object ClearState extends MonitorActorMessage

/**
 * This actor manages the monitoring of the test execution.
 * It contains the set of test messages defined by the user
 * and uses this set to match with the messages sent/received by TestActorRef
 * For the efficiency, the actor just keeps track of the messages that match
 * with the user defined test messages.
 * It can report which test messages delivered/processed and how many times
 * they are delivered/processed.
 *
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 */

class TraceMonitorActor() extends Actor {

  var testMessagesInfo = new HashMap[TestMessageEnvelop, Array[Int]]()
  var deliveredAsyncMessages = new ArrayBuffer[RealMessageEnvelop]()
  var messageTrace = new ListBuffer[TestMessageEnvelop]
  var listener: Listener = null

  def receive =
    {
      case AddTestMessageEnvelop(testMessage) ⇒ {
        testMessagesInfo.put(testMessage, Array(0, 0))
        self.reply()
      }
      case AsyncMessageEvent(message, event) ⇒ {
        val matchedTestMessageEnvelops = testMessagesInfo.filterKeys(m ⇒ m.matchWithRealEnvelop(message))
        for ((testMsg, dp) ← matchedTestMessageEnvelops) {
          event match {
            case Delivered ⇒ {
              deliveredAsyncMessages.+=(message)
              testMessagesInfo.update(testMsg, Array(dp(0) + 1, dp(1)))
            }
            case Processed ⇒ {
              testMessagesInfo.update(testMsg, Array(dp(0), dp(1) + 1))
              val index = deliveredAsyncMessages.indexWhere(m ⇒ m == message)
              if (index >= 0) deliveredAsyncMessages.remove(index)
              if (message.message.equals("Reply")) log("reply" + index + " " + testMessagesInfo(testMsg)(1))
              log("received processing: " + message.message + " " + message.receiver)
            }

          }
        }
        if (matchedTestMessageEnvelops.size > 0 && listener != null) {
          notifyListener()
        }
      }
      case ReplyMessageEvent(message) ⇒ {
        //        messageTrace.+=(message)
        val matchedTestMessageEnvelops = testMessagesInfo.filterKeys(m ⇒ m.matchWithRealEnvelop(message))
        for ((testMsg, dp) ← matchedTestMessageEnvelops) {
          testMessagesInfo.update(testMsg, Array(dp(0) + 1, dp(1) + 1))
        }

      }

      case NotifyMeForMessageEvent(testMessages, event) ⇒ {
        listener = Listener(self.channel, testMessages, event)
        notifyListener()
      }
      /**
       * returns the set of the real  messages that are matched with the test message and the specified event
       */
      case MatchedMessageEventCount(testMessage, event) ⇒ {
        event match {
          case Delivered ⇒ self.reply(testMessagesInfo(testMessage)(0))
          case Processed ⇒ {
            self.reply(testMessagesInfo(testMessage)(1))
            log(testMessage.message + " " + testMessagesInfo(testMessage)(1) + " " + testMessagesInfo(testMessage)(0))
          }
        }
      }
      case AllDeliveredMessagesAreProcessed ⇒ self.reply(deliveredAsyncMessages.size == 0)
      case NotProcessedMessages             ⇒ self.reply(deliveredAsyncMessages)

      case ClearState ⇒ {
        testMessagesInfo = new HashMap[TestMessageEnvelop, Array[Int]]()
        deliveredAsyncMessages = new ArrayBuffer[RealMessageEnvelop]()
        messageTrace = new ListBuffer[TestMessageEnvelop]
        self.reply()

      }

    }

  private def notifyListener() {
    listener.event match {
      case Processed ⇒ {
        if ((listener.testMessages != null && allListenerTestMessagesAreProcessed) ||
          (listener.testMessages == null && allTestMessagesAreProcessed)) {
          listener.channel ! true
          listener = null

        }
      }
      case Delivered ⇒ {
        if ((listener.testMessages != null && allListenerTestMessagesAreDelivered) ||
          (listener.testMessages == null && allTestMessagesAreDelivered)) {
          listener.channel ! true
          listener = null

        }

      }
    }

  }

  private def allTestMessagesAreProcessed(): Boolean = {
    for ((message, Array(deliverCount, processCount)) ← testMessagesInfo) {
      if (processCount == 0) return false
    }
    return true
  }
  private def allListenerTestMessagesAreProcessed(): Boolean = {
    for (message ← listener.testMessages) {
      if (testMessagesInfo(message)(1) == 0) return false
    }
    return true
  }
  private def allListenerTestMessagesAreDelivered(): Boolean = {
    for (message ← listener.testMessages) {
      if (testMessagesInfo(message)(0) == 0) return false
    }
    return true
  }

  private def allTestMessagesAreDelivered(): Boolean = {
    for ((message, Array(deliverCount, processCount)) ← testMessagesInfo) {
      if (deliverCount == 0) return false
    }
    return true
  }

  //for debugging only
  private var debug = false
  private def log(s: String) = if (debug) println(s)

}
case class Listener(channel: UntypedChannel, testMessages: Set[TestMessageEnvelop], event: MessageEventType)