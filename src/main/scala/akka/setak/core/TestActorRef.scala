/**
 * Copyright (C) 2011 Samira Tasharofi
 */

package akka.setak.core

import akka.actor._
import java.net.InetSocketAddress
import akka.dispatch.MessageInvocation
import monitor._
import akka.dispatch._
import java.util.concurrent.atomic.AtomicReference
import com.eaio.uuid.UUID
import akka.dispatch.Future
import scala.collection.mutable.HashSet

/**
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 */

class TestActorRef(
  private[this] val actorFactory: () ⇒ Actor,
  val _homeAddress: Option[InetSocketAddress],
  traceMonitorActor: ActorRef)
  extends LocalActorRef(actorFactory, _homeAddress, false) {
  import MessageEventEnum._

  /**
   * A container for the messges that should be posted to the mailbox later
   */
  @volatile
  private var _cloudMessages = new HashSet[RealMessageInvocation]()

  /**
   * A set of partial orders between the messages. It is used to remove some nondeterminism from the execution.
   * TestSchedule is thread-safe.
   */
  @volatile
  private var _currentSchedule: TestSchedule = null

  /**
   * Callback for the Dispatcher. Informs the monitor actor about processing a message.
   */
  override def invoke(messageHandle: MessageInvocation): Unit = {
    try {
      super.invoke(messageHandle)
    } finally {
      traceMonitorActor ! AsyncMessageEvent(new RealMessageInvocation(messageHandle.receiver, messageHandle.message, messageHandle.channel), MessageEventEnum.Processed)
      log("sent processing" + messageHandle.message)

    }
  }

  /**
   * Overrides the reply method to keep track of the messages sent to the ActorCompletableFutures
   */
  override def reply(message: Any) = {
    if (channel.isInstanceOf[ActorCompletableFuture]) {
      traceMonitorActor ! ReplyMessageEvent(new RealMessageInvocation(channel, message, this))
    }
    super.reply(message)
  }

  /**
   *
   * Overrides the tryReply method to keep track of the messages sent to the ActorCompletableFutures
   */
  override def tryReply(message: Any): Boolean = {
    if (channel.isInstanceOf[ActorCompletableFuture]) {
      traceMonitorActor ! ReplyMessageEvent(new RealMessageInvocation(channel, message, this))
    }
    super.tryTell(message)
  }

  /**
   * @return reference to the actor object, where the static type matches the factory used inside the
   * constructor. This reference is discarded upon restarting the actor
   */
  def actorObject[T <: Actor]: T = actorInstance.asInstanceOf[AtomicReference[T]].get

  /**
   * Overrides the postMessageToMailbox to apply the constraints in the schedule if there is any
   */
  override protected[akka] def postMessageToMailbox(message: Any, channel: UntypedChannel): Unit = {
    if (_currentSchedule == null) postMessageToMailboxWithoutCheck(message, channel)
    else {
      postMessageBySchedule(message, channel)
    }
  }

  /**
   * Calls the postMessageToMailbox without checking any condition and informs the monitor actor about
   * the delivery of a message
   */
  private def postMessageToMailboxWithoutCheck(message: Any, channel: UntypedChannel): Unit = {
    super.postMessageToMailbox(message, channel)
    traceMonitorActor ! AsyncMessageEvent(new RealMessageInvocation(this, message, channel), Delivered)
  }

  /**
   * It checks the position of the message in the schedule schedule:
   * 1) if the message is not in the schedule then it calls postMessageToMailboxWithoutCheck
   * 2) if the message  or it is in the head of the schedule it calls postMessageToMailboxWithoutCheck and
   * removes the message from the head of the schedule
   * 3) if the message is somewhere in the schedule other than the head, it keeps the message in the cloud
   */
  private def postMessageBySchedule(message: Any, channel: UntypedChannel) = synchronized {
    val invocation = new RealMessageInvocation(this, message, channel)
    log("message index:" + message + " " + _currentSchedule.leastIndexOf(invocation))
    _currentSchedule.leastIndexOf(invocation) match {
      case -1 ⇒ postMessageToMailboxWithoutCheck(message, channel)
      case 0 ⇒ {
        _currentSchedule.removeFromHead(invocation)
        log("removeFromSchedule: " + invocation.message)
        postMessageToMailboxWithoutCheck(message, channel)
        checkForDeliveryFromCloud()
      }
      case _ ⇒ _cloudMessages.add(invocation) //; println("added to cloud" + invocation)
    }

  }

  /**
   * Overrides the postMessageToMailboxAndCreateFutureResultWithTimeout to
   * apply the constraints in the schedule if there is any
   */
  override protected[akka] def postMessageToMailboxAndCreateFutureResultWithTimeout(
    message: Any,
    timeout: Long,
    channel: UntypedChannel): ActorCompletableFuture = {
    if (_currentSchedule == null) postMessageToMailboxAndCreateFutureResultWithTimeoutWithoutCheck(message, timeout, channel)
    else {
      postMessageAndCreateFutureBySchedule(message, timeout, channel)

    }
  }

  /**
   * Calls the postMessageToMailboxAndCreateFutureResultWithTimeoutWithoutCheck without checking
   * any condition and informs the monitor actor about the delivery of a message
   */
  private def postMessageToMailboxAndCreateFutureResultWithTimeoutWithoutCheck(
    message: Any,
    timeout: Long,
    channel: UntypedChannel): ActorCompletableFuture = {
    val future = super.postMessageToMailboxAndCreateFutureResultWithTimeout(message, timeout, channel)
    traceMonitorActor ! AsyncMessageEvent(new RealMessageInvocation(this, message, future.asInstanceOf[ActorCompletableFuture]), Delivered)
    future
  }

  /**
   * It creates a future for the sender of the invocation
   */
  private def createFuture(
    timeout: Long,
    channel: UntypedChannel): ActorCompletableFuture = if (isRunning) {
    val future = channel match {
      case f: ActorCompletableFuture ⇒ f
      case _                         ⇒ new ActorCompletableFuture(timeout)(dispatcher)
    }
    future
  } else throw new ActorInitializationException("Actor has not been started, you need to invoke 'actor' before using it")

  /**
   * It checks the position of the message in the schedule schedule:
   * 1) if the message is not in the schedule then it calls postMessageToMailboxAndCreateFutureResultWithTimeoutWithoutCheck
   * 2) if the message  or it is in the head of the schedule it calls postMessageToMailboxAndCreateFutureResultWithTimeoutWithoutCheck,
   * removes the message from the head of the schedule, and returns the future
   * 3) if the message is somewhere in the schedule other than the head, it creates the future, keeps the message in the cloud and
   * returns the future
   */
  private def postMessageAndCreateFutureBySchedule(message: Any, timeout: Long, channel: UntypedChannel): ActorCompletableFuture = synchronized {

    var invocation = new RealMessageInvocation(this, message, channel)
    log("message index:" + message + " " + _currentSchedule.leastIndexOf(invocation))
    _currentSchedule.leastIndexOf(invocation) match {
      case -1 ⇒ postMessageToMailboxAndCreateFutureResultWithTimeoutWithoutCheck(message, timeout, channel)
      case 0 ⇒ {
        val result = _currentSchedule.removeFromHead(invocation)
        log("removeFromSchedule: " + invocation.message)
        val future = postMessageToMailboxAndCreateFutureResultWithTimeoutWithoutCheck(message, timeout, channel)
        checkForDeliveryFromCloud()
        future
      }
      case _ ⇒ {
        val newTimeout = timeout + akka.setak.TestConfig.sleepInterval * akka.setak.TestConfig.sleepInterval
        //(akka.setak.TestConfig.sleepInterval^2 * (akka.setak.TestConfig.maxTryForStability ^ 2 + akka.setak.TestConfig.maxTryForStability)  )
        val future = createFuture(newTimeout, channel)
        invocation = new RealMessageInvocation(this, message, future)
        _cloudMessages.add(invocation)
        log("added to cloud" + invocation + " " + newTimeout)
        //println("new "+message.toString()+newTimeout)
        future
      }
    }

  }

  /**
   * Checks for the further delivery from the messages in the cloud. This method is synchronized by caller.
   */
  private def checkForDeliveryFromCloud() {
    var delivered = deliverFromCloud()
    while (delivered) {
      delivered = deliverFromCloud()
    }
  }

  /**
   * Checks if there is any message in the cloud that can be delivered.
   * In the case that there is a message in cloud which is in the head of any partial orders in the
   * schedule or the schedule is empty, it posts the message into the mailbox,
   * removes the message from the cloud, (and) updates the schedule (which returns true).
   * In the case that nothing from the cloud can be delivered, it returns false.
   */
  private def deliverFromCloud(): Boolean = {

    if (_currentSchedule.isEmpty) {
      for (invocation ← _cloudMessages) {
        _cloudMessages.-=(invocation)
        postMessageToMailboxWithoutCheck(invocation.message, invocation.sender)
        return true

      }
    }

    for (invocation ← _cloudMessages) {
      if (_currentSchedule.leastIndexOf(invocation) == 0) {
        _cloudMessages.-=(invocation)
        log("removed from cloud" + _cloudMessages.size)
        val result = _currentSchedule.removeFromHead(invocation)
        postMessageToMailboxWithoutCheck(invocation.message, invocation.sender)
        return result

      }
    }
    false

  }

  /**
   * Adds a partial order between the message to the schedule
   */
  def addPartialOrderToSchedule(po: TestMessageSequence) = synchronized {
    if (_currentSchedule == null) _currentSchedule = new TestSchedule(Set(po))
    else _currentSchedule.addPartialOrder(po)
    log("current schedule= " + _currentSchedule.toString())
  }

  /**
   * It is called to make sure that the specified schedule happened
   */
  def scheduleHappened = synchronized {
    _currentSchedule == null || _currentSchedule.isEmpty
  }

  /**
   * It is called when checking for stable state to make sure that all the messages in the cloud are finally delivered.
   */
  def cloudIsEmpty = synchronized {
    _cloudMessages.isEmpty
  }
  private var debug = false
  private def log(s: String) = if (debug) println(s)

}

