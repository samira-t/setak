/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak.examples

import akka.actor.Actor
import akka.actor.ActorRef
import org.junit.Test
import org.junit.Before
import org.junit.After
import akka.setak.core.TestMessageEnvelop
import akka.setak.core.TestMessageEnvelopSequence._
import akka.setak.core.TestActorRef
import akka.setak.SetakJUnit
import akka.setak.SetakFlatSpec
import akka.setak.Commons._

abstract class BufferMessage
case class Put(x: Int) extends BufferMessage
case object Get extends BufferMessage

abstract class ProducerMessage
case class Produce(values: List[Int]) extends ProducerMessage

abstract class ConsumerMessage
case class Consume(count: Int) extends ConsumerMessage
case object GetToken extends ConsumerMessage

class BoundedBuffer(size: Int) extends Actor {

  var content = new Array[Int](size)
  var head, tail, curSize = 0

  def receive = {
    case Put(x) ⇒ if (curSize < size) {
      content(tail) = x
      tail = (tail + 1) % size
      curSize += 1

    }
    case Get ⇒ if (curSize > 0) {
      val r = content(head)
      head = (head + 1) % size
      curSize -= 1
      self.reply(r)
    } else self.reply(-2)
  }
}

class Consumer(buf: ActorRef) extends Actor {
  var token: Int = -1

  def receive = {
    case Consume(count) ⇒ {
      for (i ← 1 to count) {
        token = (buf ? Get).get.asInstanceOf[Int]
      }
    }
    case GetToken ⇒ self.reply(token)
  }

}

class Producer(buf: ActorRef) extends Actor {
  def receive = {
    case Produce(values) ⇒ {
      values.foreach(v ⇒ buf ! Put(v))
    }
  }
}

class JUnitTestBoundedBuffer extends SetakJUnit {

  var buf: TestActorRef = _
  var put: TestMessageEnvelop = _

  @Before
  def setUp() {
    buf = actorOf(new BoundedBuffer(1)).start
    put = testMessagePatternEnvelop(anyActorRef, buf, { case Put(_) ⇒ })
  }

  @Test
  def testPutGet() {
    buf ! Put(12)

    afterMessages(put) {
      assert(buf.actorObject[BoundedBuffer].curSize == 1, "The current size is not one")
    }

    val getValue = (buf ? Get).mapTo[Int].get
    assert(getValue == 12, "The returned value is not 12")
  }

  @Test
  def testTwoPuts() {

    buf ! Put(1)
    buf ! Put(2)

    whenStable {
      assert(buf.actorObject[BoundedBuffer].curSize == 1, "The current size is not one")
      assert(processingCount(put) == 2, "Put has not been processed twice")
    }

  }
}

class SpecTestBoundedBuffer extends SetakFlatSpec {

  var buf: TestActorRef = _
  var put: TestMessageEnvelop = _

  override def setUp() {
    buf = actorOf(new BoundedBuffer(1)).start
    put = testMessagePatternEnvelop(anyActorRef, buf, { case Put(_) ⇒ })
  }

  "the current size of buffer" should "be one and returns value 12" in {
    buf ! Put(12)

    whenStable {
      assert(buf.actorObject[BoundedBuffer].curSize == 1)
    }

    val getValue = (buf ? Get).mapTo[Int].get
    assert(getValue == 12)
  }

  "Both Put messages" should "be processed and only the first one should update the content" in {

    buf ! Put(1)
    buf ! Put(2)

    whenStable {
      assert(buf.actorObject[BoundedBuffer].curSize == 1, "The current size is not one")
      assert(processingCount(put) == 2, "Put has not been processed twice")
    }

  }
}
class TestBoundedBufferWithProducerConsumer extends SetakJUnit {

  @Test
  def testBufferWithProducerConsumer() {
    val buf = actorOf(new BoundedBuffer(1)).start
    val consumer = actorOf(new Consumer(buf)).start
    val producer = actorOf(new Producer(buf)).start

    val put1 = testMessageEnvelop(producer, buf, Put(1))
    val get = testMessageEnvelop(anyActorRef, buf, Get)

    //step 1
    consumer ! Consume(1)

    whenStable {
      assert((consumer ? GetToken).mapTo[Int].get == -2)
    }

    //step 2
    setSchedule(put1 -> get)
    producer ! Produce(List(1))
    consumer ! Consume(1)

    afterMessages(get) {
      assert((consumer ? GetToken).mapTo[Int].get == 1)
    }

  }
}
class TestBoundedBufferWithProducerConsumerUsingAfterMessage extends SetakJUnit {

  @Test
  def testBufferWithProducerConsumer() {
    val buf = actorOf(new BoundedBuffer(1)).start
    val consumer = actorOf(new Consumer(buf)).start
    val producer = actorOf(new Producer(buf)).start

    val put1 = testMessageEnvelop(producer, buf, Put(1))
    val get1 = testMessageEnvelop(anyActorRef, buf, Get)
    val get2 = testMessageEnvelop(anyActorRef, buf, Get)

    //step 1
    consumer ! Consume(1)

    afterMessages(get1) {
      assert((consumer ? GetToken).mapTo[Int].get == -2)
    }

    //step 2
    setSchedule(put1 -> get2)
    producer ! Produce(List(1))
    consumer ! Consume(1)

    afterAllMessages {
      assert((consumer ? GetToken).mapTo[Int].get == 1)
    }

  }
}

