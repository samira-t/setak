/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak.core
import akka.dispatch.MessageInvocation
import scala.collection.mutable.ListBuffer
import akka.actor.UntypedChannel
import akka.setak.Commons._

case class RealMessageInvocation(_reciever: UntypedChannel, _message: Any, _sender: UntypedChannel) {
  def receiver = _reciever
  def message = _message
  def sender = _sender

  def ==(otherInvocation: RealMessageInvocation): Boolean =
    (receiver == otherInvocation.receiver) && (sender == otherInvocation.sender) && (message == otherInvocation.message)
}

/**
 * This enumeration defines two different kinds of the events for the
 * messages:
 * delivered(in the mailbox), processed (removed from the mail box and executed)
 *
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 */
object MessageEventEnum extends Enumeration {
  type MessageEventType = Value
  val Delivered, Processed = Value
}

/**
 * Each test message invocation is a message defined by the user and
 * can be matched with real messages during the execution.
 *
 * The message property in the test message invocation
 * can be an object or a pattern (partial function)
 *
 * The wild card for the sender and receiver is anyActorRef.
 * The wild card for the message is anyMessage.
 *
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 */
class TestMessage {

  var _receiver: UntypedChannel = null
  var _sender: UntypedChannel = null
  var _message: Any = null
  var _messagePattern: PartialFunction[Any, Any] = null

  private def this(sender: UntypedChannel, receiver: UntypedChannel) {
    this()
    this._sender = sender
    this._receiver = receiver
  }

  def this(sender: UntypedChannel, receiver: UntypedChannel, message: Any) {
    this(sender, receiver)
    this._message = message
  }

  def this(sender: UntypedChannel, receiver: UntypedChannel, messagePattern: PartialFunction[Any, Any]) {
    this(sender, receiver)
    this._messagePattern = messagePattern
  }

  def receiver = _receiver

  def sender = _sender

  def message = _message

  def messagePattern = _messagePattern

  def matchWithRealInvocation(realInvocation: RealMessageInvocation): Boolean = {
    log("matching " + this.toString() + " " + realInvocation.toString())
    if (!compareChannels(this.sender, realInvocation.sender)) return false

    if (!compareChannels(this.receiver, realInvocation.receiver)) return false

    if (this.message != null && this.message != anyMessage && this.message != realInvocation.message) return false

    if (this.messagePattern != null && !this.messagePattern.isDefinedAt(realInvocation.message)) return false
    log(" returns true")
    return true
  }

  private def compareChannels(ch1: UntypedChannel, ch2: UntypedChannel): Boolean = {
    (ch1 == anyActorRef) || (ch2 == anyActorRef) || (ch1 == ch2)
  }

  /**
   * This operator can be applied to test messages to create a sequence(order) of the test messages which can be used for
   * deterministic execution via "setScheudle" API.
   */
  /*  def ->(testMessage: TestMessage): TestMessageSequence = {
    return (new TestMessageSequence(this) -> testMessage)
  }
*/
  override def toString(): String = "(" + sender + "," + receiver + "," + (if (message != null) message else messagePattern) + ")"

  //only for debugging
  private var debug = false
  private def log(s: String) = if (debug) println(s)

}

/**
 * Each test message invocation sequence is an ordered set of test message
 * invocations. The sequence is defined by using '->' operator.
 * The assumption is that the sender of all messages in a given sequence are the same.
 *
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 */
class TestMessageSequence(testMessage: TestMessage) {

  protected var _messageSequence = ListBuffer[TestMessage](testMessage)

  def ->(testMessage: TestMessage): TestMessageSequence = {
    _messageSequence.+=(testMessage)
    return this
  }

  def head: TestMessage = _messageSequence.headOption.orNull

  def removeHead: Boolean = {
    if (!_messageSequence.isEmpty) {
      _messageSequence.-=(_messageSequence.head)
      return true
    }
    return false
  }

  def messageSequence = _messageSequence

  def isEmpty: Boolean = {
    return _messageSequence.isEmpty
  }

  def indexOf(invocation: RealMessageInvocation): Int = {
    return _messageSequence.findIndexOf(m ⇒ m.matchWithRealInvocation(invocation))
  }

  def equals(otherSequence: TestMessageSequence): Boolean = {
    for (msg ← _messageSequence) {
      if (!otherSequence.head.equals(msg)) return false
    }
    return true
  }

}

object TestMessageSequence {
  implicit def toSequence(testMessage: TestMessage): TestMessageSequence = new TestMessageSequence(testMessage)
}