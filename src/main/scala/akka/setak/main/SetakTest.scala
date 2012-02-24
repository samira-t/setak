/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak
import core.TestExecutionManager
import core.monitor.TraceMonitorActor
import util.TestMessageEnvelopUtil
import core.TestMessageEnvelop
import core.TestActorRef
import util.TestActorRefFactory
import core.TestMessageEnvelopSequence
import akka.actor.Actor
import akka.japi.Creator
import akka.actor.UntypedChannel

trait SetakTest {

  var traceMonitorActor = akka.actor.Actor.actorOf[TraceMonitorActor].start()
  var testExecutionManager = new TestExecutionManager(traceMonitorActor)
  var testMessageUtil = new TestMessageEnvelopUtil(traceMonitorActor)
  var anonymousSchedule: TestMessageEnvelopSequence = null
  var testActorRefFactory = new TestActorRefFactory(this)
  Commons.testFactoryPool.add(testActorRefFactory)
  var testInitialized = true

  def superBeforeEach() {
    if (!testInitialized) {
      traceMonitorActor = akka.actor.Actor.actorOf[TraceMonitorActor].start()
      testExecutionManager = new TestExecutionManager(traceMonitorActor)
      testMessageUtil = new TestMessageEnvelopUtil(traceMonitorActor)
      testActorRefFactory = new TestActorRefFactory(this)
      Commons.testFactoryPool.add(testActorRefFactory)
      anonymousSchedule = null
      testInitialized = true
    }
  }

  def superAfterEach() {
    testExecutionManager.stopTest
    Commons.testFactoryPool.remove(this)
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
   * Waits for a test message to be processed and then executes the body which is usually a set of
   * assertion statements.
   */
  def afterMessages(testMessages: TestMessageEnvelop*)(body: ⇒ Unit) {
    val processed = testExecutionManager.waitForMessage(testMessages.toSet[TestMessageEnvelop])
    if (processed) {
      body
    } else throw new Exception("The message didn't get processed")
  }

  /**
   * Waits for all test messages to be processed and then executes the body which is usually a set of
   * assertion statements.
   */
  def afterAllMessages(body: ⇒ Unit) {
    val processed = testExecutionManager.waitForAllMessages()
    if (processed) {
      body
    } else throw new Exception("Some of the messages didn't get processed")
  }
  /**
   * After the timeout, executes the body that usually contains some assertions.
   */
  def afterTimeOut(timeout: Long)(body: ⇒ Unit) = {
    Thread.sleep(timeout)
    body
  }

  /*
   * testMessageUtil API calls
   */
  def testMessageEnvelop(sender: UntypedChannel, receiver: UntypedChannel, message: Any) =
    testMessageUtil.testMessageEnvelop(sender, receiver, message)

  def testMessagePatternEnvelop(sender: UntypedChannel, receiver: UntypedChannel, messagePattern: PartialFunction[Any, Any]) =
    testMessageUtil.testMessagePatternEnvelop(sender, receiver, messagePattern)

  /**
   * Checks if the message is delivered or not by asking from trace monitor actor.
   */
  def isDelivered(testMessage: TestMessageEnvelop) = testMessageUtil.isDelivered(testMessage)

  /**
   * @return the number of the test messages delivered.
   */
  def deliveryCount(testMessage: TestMessageEnvelop) = testMessageUtil.deliveryCount(testMessage)

  /**
   * Checks if the message is processed by asking from trace monitor actor.
   */
  def isProcessed(testMessage: TestMessageEnvelop) = testMessageUtil.isProcessed(testMessage)

  /**
   * @return the number of the test messages processed.
   */
  def processingCount(testMessage: TestMessageEnvelop) = testMessageUtil.processingCount(testMessage)

  /**
   * API for constraining the schedule of test execution and removing some non-determinism by specifying
   * a set of partial orders between the messages. The receivers of the messages in each partial order should
   * be the same (an instance of TestActorRef)
   */
  def setSchedule(partialOrders: TestMessageEnvelopSequence*) {
    /*
     * TODO: check if the receivers of all messages in each partial order are the same
     */
    for (po ← partialOrders) {
      if (po.head._receiver.isInstanceOf[TestActorRef]) {
        po.head._receiver.asInstanceOf[TestActorRef].addPartialOrderToSchedule(po)
      } else if (po.head._receiver == Commons.anyActorRef) {
        anonymousSchedule = po
      } else {
        throw new Exception("The receiver of the test message in a schedule should be anyActorRef or an instance of TestActorRef")
      }
    }
  }

  /*
   * testActorRefFactory API calls
   */
  def actorOf[T <: Actor: Manifest] = testActorRefFactory.actorOf[T]

  def actorOf[T <: Actor](clazz: Class[T]) = testActorRefFactory.actorOf(clazz)

  def actorOf[T <: Actor](factory: ⇒ T) = testActorRefFactory.actorOf(factory)

}