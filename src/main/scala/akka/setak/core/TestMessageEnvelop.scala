/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak.core
import scala.collection.mutable.ListBuffer
import akka.actor.UntypedChannel
import akka.setak.Commons._

case class RealMessageEnvelop(_reciever: UntypedChannel, _message: Any, _sender: UntypedChannel) {
  def receiver = _reciever
  def message = _message
  def sender = _sender

  def ==(otherEnvelop: RealMessageEnvelop): Boolean =
    (receiver == otherEnvelop.receiver) && (sender == otherEnvelop.sender) && (message == otherEnvelop.message)
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
 * Each test message envelop is a message defined by the user and
 * can be matched with real messages during the execution.
 *
 * The message property in the test message envelop
 * can be an object or a pattern (partial function)
 *
 * The wild card for the sender and receiver is anyActorRef.
 * The wild card for the message is anyMessage.
 *
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 */
class TestMessageEnvelop {

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

  def matchWithRealEnvelop(realEnvelop: RealMessageEnvelop): Boolean = {
    log("matching " + this.toString() + " " + realEnvelop.toString())
    if (!compareChannels(this.sender, realEnvelop.sender)) return false

    if (!compareChannels(this.receiver, realEnvelop.receiver)) return false

    if (this.message != null && this.message != anyMessage && this.message != realEnvelop.message) return false

    if (this.messagePattern != null && !this.messagePattern.isDefinedAt(realEnvelop.message)) return false
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
  /*  def ->(testMessage: TestMessageEnvelop): TestMessageEnvelopSequence = {
    return (new TestMessageEnvelopSequence(this) -> testMessage)
  }
*/
  override def toString(): String = "(" + sender + "," + receiver + "," + (if (message != null) message else messagePattern) + ")"

  //only for debugging
  private var debug = false
  private def log(s: String) = if (debug) println(s)

}

/**
 * Each test message envelop sequence is an ordered set of test message
 * envelops. The sequence is defined by using '->' operator.
 * The assumption is that the sender of all messages in a given sequence are the same.
 *
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 */
class TestMessageEnvelopSequence(testMessage: TestMessageEnvelop) {

  protected var _messageSequence = ListBuffer[TestMessageEnvelop](testMessage)

  def ->(testMessage: TestMessageEnvelop): TestMessageEnvelopSequence = {
    _messageSequence.+=(testMessage)
    return this
  }

  def head: TestMessageEnvelop = _messageSequence.headOption.orNull

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

  def indexOf(envelop: RealMessageEnvelop): Int = {
    return _messageSequence.findIndexOf(m ⇒ m.matchWithRealEnvelop(envelop))
  }

  def equals(otherSequence: TestMessageEnvelopSequence): Boolean = {
    for (msg ← _messageSequence) {
      if (!otherSequence.head.equals(msg)) return false
    }
    return true
  }

}

object TestMessageEnvelopSequence {
  implicit def toSequence(testMessage: TestMessageEnvelop): TestMessageEnvelopSequence = new TestMessageEnvelopSequence(testMessage)
}