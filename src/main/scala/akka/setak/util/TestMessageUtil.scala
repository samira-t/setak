/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak.util
import akka.dispatch.MessageInvocation
import akka.setak.core.TestActorRef
import scala.collection.mutable.ListBuffer
import akka.setak.core.monitor._
import akka.setak.core.TestMessage
import akka.actor.UntypedChannel
import akka.setak.core.TestMessageSequence
import akka.setak.core.TestSchedule
import scala.collection.mutable.HashSet
import akka.setak.core.MessageEventEnum
import scala.collection.mutable.ArrayBuffer
import akka.actor.ActorRef

/**
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 */
class TestMessageUtil(traceMonitorActor: ActorRef) {

  /**
   * Factories for creating test message invocations
   * Each test message invocation is identified by sender, receiver and
   * message. The message parameter can be an object or a partial function
   * for pattern matching.
   *
   * Each defined test message is also added to the set of test messages
   * in the Monitor object so that it can set the set of test messages that
   * should be traced by the testMonitorActor.
   */
  def testMessage(sender: UntypedChannel, receiver: UntypedChannel, message: Any): TestMessage = {
    var msg = new TestMessage(sender, receiver, message)
    (traceMonitorActor ? AddTestMessage(msg)).get
    msg
  }

  def testMessagePattern(sender: UntypedChannel, receiver: UntypedChannel, messagePattern: PartialFunction[Any, Any]): TestMessage = {
    var msg = new TestMessage(sender, receiver, messagePattern)
    (traceMonitorActor ? AddTestMessage(msg)).get
    msg
  }

  val anyMessage = new Object()

  /**
   * Checks if the message is delivered or not by asking from trace monitor actor.
   */
  def isDelivered(testMessage: TestMessage): Boolean = {
    (traceMonitorActor ? MatchedMessageEventCount(testMessage, MessageEventEnum.Delivered)).mapTo[Int].get > 0
  }

  /**
   * @return the number of the test messages delivered.
   */
  def deliveryCount(testMessage: TestMessage): Int = {
    (traceMonitorActor ? MatchedMessageEventCount(testMessage, MessageEventEnum.Delivered)).mapTo[Int].get
  }

  /**
   * Checks if the message is processed by asking from trace monitor actor.
   */
  def isProcessed(testMessage: TestMessage): Boolean = {
    (traceMonitorActor ? MatchedMessageEventCount(testMessage, MessageEventEnum.Processed)).mapTo[Int].get > 0
  }

  /**
   * @return the number of the test messages processed.
   */
  def processingCount(testMessage: TestMessage): Int = {
    (traceMonitorActor ? MatchedMessageEventCount(testMessage, MessageEventEnum.Processed)).mapTo[Int].get
  }

}

