/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak
import core.TestExecutionManager
import core.monitor.TraceMonitorActor
import util.TestMessageUtil
import core.TestMessage
import core.TestActorRef
import util.TestActorRefFactory
import util.TestExecutionUtil
import core.TestMessageSequence
import akka.actor.Actor
import akka.japi.Creator
import akka.actor.UntypedChannel

trait SetakTest {

  var traceMonitorActor = akka.actor.Actor.actorOf[TraceMonitorActor].start
  var testExecutionManager = new TestExecutionManager(traceMonitorActor)
  var testMessageUtil = new TestMessageUtil(traceMonitorActor)
  var testActorRefFactory = new TestActorRefFactory(traceMonitorActor)
  var testInitialized = true

  def superBeforeEach() {
    if (!testInitialized) {
      traceMonitorActor = akka.actor.Actor.actorOf[TraceMonitorActor].start
      testExecutionManager = new TestExecutionManager(traceMonitorActor)
      testMessageUtil = new TestMessageUtil(traceMonitorActor)
      testActorRefFactory = new TestActorRefFactory(traceMonitorActor)
    }
  }

  def superAfterEach() {
    testExecutionManager.stopTest
    testInitialized = false
  }

  /**
   * Waits for the system to gets stable and then executes the body which is usually a set of
   * assertion statements.
   */
  def whenStable(body: ⇒ Unit)(implicit tryCount: Int = TestConfig.maxTryForStability) = {
    val isStable = testExecutionManager.checkForStability(tryCount)
    if (isStable) {
      body
    } else throw new Exception("The system didn't get stable")
  }

  /**
   * After the timeout, executes the body that usually contains some assertions.
   */
  def after(timeout: Long)(body: ⇒ Unit) = {
    Thread.sleep(timeout)
    body
  }

  /*
   * testMessageUtil API calls
   */
  def testMessage(sender: UntypedChannel, receiver: UntypedChannel, message: Any) =
    testMessageUtil.testMessage(sender, receiver, message)

  def testMessagePattern(sender: UntypedChannel, receiver: UntypedChannel, messagePattern: PartialFunction[Any, Any]) =
    testMessageUtil.testMessagePattern(sender, receiver, messagePattern)

  /**
   * Checks if the message is delivered or not by asking from trace monitor actor.
   */
  def isDelivered(testMessage: TestMessage) = testMessageUtil.isDelivered(testMessage)

  /**
   * @return the number of the test messages delivered.
   */
  def deliveryCount(testMessage: TestMessage) = testMessageUtil.deliveryCount(testMessage)

  /**
   * Checks if the message is processed by asking from trace monitor actor.
   */
  def isProcessed(testMessage: TestMessage) = testMessageUtil.isProcessed(testMessage)

  /**
   * @return the number of the test messages processed.
   */
  def processingCount(testMessage: TestMessage) = testMessageUtil.processingCount(testMessage)

  /*
 * TestExecutionUtil API call
 */
  /**
   * API for constraining the schedule of test execution and removing some non-determinism by specifying
   * a set of partial orders between the messages. The receivers of the messages in each partial order should
   * be the same (an instance of TestActorRef)
   */
  def setSchedule(partialOrders: TestMessageSequence*) = TestExecutionUtil.setSchedule(partialOrders.toSet)

  /*
   * testActorRefFactory API calls
   */
  def actorOf[T <: Actor: Manifest] = testActorRefFactory.actorOf[T]

  def actorOf[T <: Actor](clazz: Class[T]) = testActorRefFactory.actorOf(clazz)

  def actorOf[T <: Actor](factory: ⇒ T) = testActorRefFactory.actorOf(factory)

}