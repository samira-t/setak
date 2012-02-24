/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak.core
import akka.actor.Actor
import akka.actor.ActorRef
import monitor.NotProcessedMessages
import akka.actor.LocalActorRef
import scala.collection.mutable.HashSet
import scala.collection.mutable.ArrayBuffer
import akka.setak.TestConfig
import monitor._
import akka.setak.core.MessageEventEnum._
import akka.dispatch.FutureTimeoutException

/**
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 */
class TestExecutionManager(traceMonitorActor: ActorRef) {

  private var actorsWithMessages = new HashSet[ActorRef]

  def startTest {
  }

  /**
   * Checks for the stability of the system.
   * The system is stable if:
   * 1) the mailbox of the actors in the test are empty and they don't have any messages in their
   * cloud and their encoded schedule happened
   * 2) the monitor actor has received the confirmation of processing all the delivered messages
   * In fact, if condition 1 holds, the actor might be busy with processing the last message. The
   * second condition takes care of this situation
   *
   * It tries to check the stability of the system by giving maxTry. the default is 20 times
   * and each time it sleeps 100 milliseconds before trying again.
   *
   * @return false if the system did not get stable with that maxTry
   *
   *
   */
  def checkForStability(maxTry: Int = TestConfig.maxTryForStability): Boolean =
    {
      var triedCheck = 0
      var isStable = false
      var notProcessedMessages = new ArrayBuffer[RealMessageEnvelop]()
      var reason = ""

      while (triedCheck < maxTry && !isStable) {
        triedCheck += 1
        Thread.sleep(TestConfig.sleepInterval * triedCheck)

        isStable = true
        actorsWithMessages.clear()
        notProcessedMessages.clear()

        for (a ← Actor.registry) {
          if (a.isRunning) {
            if (a == traceMonitorActor && !a.asInstanceOf[LocalActorRef].dispatcher.mailboxIsEmpty(a.asInstanceOf[LocalActorRef])) {
              isStable = false
              reason += "***monitor has messages"
            } else if (a.isInstanceOf[TestActorRef]) {
              if (!a.asInstanceOf[TestActorRef].cloudIsEmpty || !a.asInstanceOf[TestActorRef].scheduleHappened ||
                !a.asInstanceOf[LocalActorRef].dispatcher.mailboxIsEmpty(a.asInstanceOf[LocalActorRef])) {

                isStable = false
                reason += a + a.asInstanceOf[TestActorRef].cloudIsEmpty.toString + " " + a.asInstanceOf[LocalActorRef].dispatcher.mailboxIsEmpty(a.asInstanceOf[LocalActorRef]).toString + " " +
                  a.asInstanceOf[TestActorRef].scheduleHappened.toString + "***"
              }
            }
          }
        }

        /*
         * If no message is in the mailbox of the actors check to see if they have finished with processing 
         * all of their messages. In that case, ask from monitor actor to see if the messages that 
         * have been delivered are processed.
         */
        if (isStable) {
          if (!(traceMonitorActor ? AllDeliveredMessagesAreProcessed).mapTo[Boolean].get) {
            isStable = false
            reason += "***message not processed"
            if (triedCheck == maxTry) {
              //for debugging: record the messages that are delivered but not processed yet
              notProcessedMessages = (traceMonitorActor ? NotProcessedMessages).mapTo[ArrayBuffer[RealMessageEnvelop]].get
            }
            log("not all delivered messages are processed yet")

          }
        }
      }
      if (!isStable) { log("no stable=" + reason); if (notProcessedMessages.size > 0) log(notProcessedMessages.head._message.toString()) }
      isStable

    }

  /**
   * Waits for a test message messages to be processed.
   * The timeout is specified in TestConfig.timeOutForMessages.
   *
   * @return false if the message is not processed with the timeout.
   */
  def waitForMessage(messages: Set[TestMessageEnvelop]): Boolean =
    {
      try {
        val isProcseed = traceMonitorActor.ask(NotifyMeForMessageEvent(messages, Processed), TestConfig.timeOutForMessages).mapTo[Boolean].get
      } catch {
        case ex: FutureTimeoutException ⇒ return false
      }
      return true
    }

  /**
   * waits for all test messages to be processed.
   * The timeout is specified in TestConfig.timeOutForMessages.
   *
   * @return false if the message all the test messages are not processed with the timeout.
   */
  def waitForAllMessages(): Boolean =
    {
      try {
        val isProcseed = traceMonitorActor.ask(NotifyMeForMessageEvent(null, Processed), TestConfig.timeOutForMessages).mapTo[Boolean].get
      } catch {
        case ex: FutureTimeoutException ⇒ return false
      }
      return true
    }

  /**
   * Stops each test by stopping all the actors
   */
  def stopTest {
    log("stability for stopping the test")
    // with a timeout, waits for the system to get stable 
    val stable = checkForStability()

    //Stop other actors including the monitor actor
    for (actor ← Actor.registry) {
      if (actor.isInstanceOf[TestActorRef] && actor.isRunning) {
        actor.stop
      }
    }
    traceMonitorActor.stop

  }

  private var debug = false
  private def log(s: String) = if (debug) println(s)
}