/**
 * Copyright (C) 2009-2011 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.setack.test

import akka.actor.Actor
import akka.actor.ActorRef
import org.junit.Test
import org.junit.Before
import org.junit.After
import akka.setack.core.TestMessageInvocation
import akka.setack.core.TestMessageInvocationSequence._
import akka.setack.core.TestActorRef
import akka.setack.SetackJUnit
import akka.setack.Commons._

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
    case msg @ Put(x) ⇒ if (curSize < size) {
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

class BuggyBoundedBuffer(size: Int) extends Actor {

  var content = new Array[Int](size)
  var head, tail, curSize = 0

  def receive = {
    case msg @ Put(x) ⇒ if (curSize <= size) {
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

class TestBoundedBuffer extends SetackJUnit with org.scalatest.junit.JUnitSuite {
  var buf: TestActorRef = _
  var consumer: TestActorRef = _
  var producer: TestActorRef = _

  var put1: TestMessageInvocation = _
  var put2: TestMessageInvocation = _
  var put3: TestMessageInvocation = _
  var get: TestMessageInvocation = _

  @Before
  def setUp() {
    buf = actorOf(new BoundedBuffer(1)).start
    consumer = actorOf(new Consumer(buf)).start
    producer = actorOf(new Producer(buf)).start

    put1 = testMessage(producer, buf, Put(1))
    put2 = testMessage(producer, buf, Put(2))
    put3 = testMessage(producer, buf, Put(3))
    get = testMessage(anyActorRef, buf, Get)
  }

  @Test
  def testNormalBuffer() {
    setSchedule(put1 -> get)
    producer ! Produce(List(1))
    consumer ! Consume(1)
    // Phase1
    whenStable {
      assert((consumer ? GetToken).mapTo[Int].get == 1)
    }

    //Phase 2
    setSchedule(put3 -> put2 -> get)
    producer ! Produce(List(2, 3))
    consumer ! Consume(1)
    //println(consumer.isRunning + " " + consumer.isShutdown + " " + consumer.isUnstarted)

    whenStable {
      assert((consumer ? GetToken).mapTo[Int].get == 3)
    }
  }

  @Test
  def testEmptyBuffer() {
    setSchedule(get -> put1)
    producer ! Produce(List(1))
    consumer ! Consume(1)

    // Phase1
    whenStable {
      assert((consumer ? GetToken).mapTo[Int].get == -2)
    }
  }

  @Test
  def testFullBuffer() {
    setSchedule(put2 -> put1)
    producer ! Produce(List(1, 2))

    // Phase1
    whenStable {
      assert(buf.actorObject[BoundedBuffer].curSize == 1)
    }

    //Phase 2
    consumer ! Consume(1)
    whenStable {
      assert(consumer.actorObject[Consumer].token == 2)
    }
  }
}