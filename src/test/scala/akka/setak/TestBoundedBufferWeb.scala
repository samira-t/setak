/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak.test

import akka.actor.Actor
import akka.actor.ActorRef
import org.junit.Test
import org.junit.Before
import org.junit.After
import akka.setak.core.TestMessage
import akka.setak.core.TestMessageSequence._
import akka.setak.core.TestActorRef
import akka.setak.SetakJUnit
import akka.setak.SetakFlatSpec
import akka.setak.Commons._
class JUnitTestBoundedBuffer extends SetakJUnit {

  var buf: TestActorRef = _
  var put: TestMessage = _

  @Before
  def setUp() {
    buf = actorOf(new BoundedBuffer(1)).start
    put = testMessagePattern(anyActorRef, buf, { case Put(_) ⇒ })
  }

  @Test
  def testPutGet() {
    buf ! Put(12)

    whenStable {
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
  var put: TestMessage = _

  override def setUp() {
    buf = actorOf(new BoundedBuffer(1)).start
    put = testMessagePattern(anyActorRef, buf, { case Put(_) ⇒ })
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
  var buf: TestActorRef = _
  var consumer: TestActorRef = _
  var producer: TestActorRef = _

  var put: TestMessage = _
  var get: TestMessage = _

  @Test
  def testBufferWithProducerConsumer1() {
    val buf = actorOf(new BoundedBuffer(1)).start
    val consumer = actorOf(new Consumer(buf)).start
    val producer = actorOf(new Producer(buf)).start

    val put = testMessagePattern(producer, buf, { case Put(_) ⇒ })
    val get = testMessage(anyActorRef, buf, Get)

    //step 1
    setSchedule(put -> get)
    producer ! Produce(List(1))
    consumer ! Consume(1)

    whenStable {
      assert((consumer ? GetToken).mapTo[Int].get == 1)
    }

    //step 2
    consumer ! Consume(1)

    whenStable {
      assert((consumer ? GetToken).mapTo[Int].get == -2)
    }

  }
}

/*
class TestBoundedBuffer extends SetakJUnit {
  var buf: TestActorRef = _
  var consumer: TestActorRef = _
  var producer: TestActorRef = _

  var put1: TestMessage = _
  var put2: TestMessage = _
  var put3: TestMessage = _
  var get: TestMessage = _

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
}*/ 