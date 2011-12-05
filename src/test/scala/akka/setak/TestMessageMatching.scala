/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak.test

import akka.setak._

import org.junit.Test
import org.junit.Before
import org.junit.After
import akka.setak.core.TestMessage
import akka.setak.core.TestActorRef
import akka.setak.Commons._
import akka.actor.Actor
import scala.collection.mutable.HashSet

case class IntMessage(x: Int)
case class BooleanMessage(b: Boolean)

class IntBoolActor extends Actor {
  var intMessages = new HashSet[Int]
  var boolMessages = new HashSet[Boolean]
  def receive = {
    case IntMessage(x)     ⇒ intMessages.add(x)
    case BooleanMessage(b) ⇒ boolMessages.add(b)
  }
}

class JUnitTestMessageMatching extends SetakJUnit {

  var a: TestActorRef = null
  var intAny: TestMessage = null
  var int1: TestMessage = null
  var btrue: TestMessage = null
  var bAny: TestMessage = null

  @Before
  def setUp {
    a = actorOf(new IntBoolActor()).start
    int1 = testMessage(anyActorRef, a, IntMessage(1))
    intAny = testMessagePattern(anyActorRef, a, { case IntMessage(_) ⇒ })
    btrue = testMessage(anyActorRef, a, BooleanMessage(true))
    bAny = testMessagePattern(anyActorRef, a, { case BooleanMessage(_) ⇒ })
  }

  @Test
  def test1 {
    a ! IntMessage(5)
    a ! IntMessage(1)
    a ! BooleanMessage(false)
    a ! BooleanMessage(false)

    whenStable {
      assert(isDelivered(int1))
      assert(isProcessed(intAny))
      assert(deliveryCount(intAny) == 2)
      assert(processingCount(bAny) == 2)
    }
  }

  @After
  def tearDown {
    a.stop
  }

}

class ScalaTestMessageMatching extends SetakFlatSpec with org.scalatest.matchers.ShouldMatchers {

  var testActor: TestActorRef = null
  var intAny: TestMessage = null
  var int1: TestMessage = null
  var btrue: TestMessage = null
  var bAny: TestMessage = null

  override def setUp {
    testActor = actorOf[IntBoolActor].start
    int1 = testMessage(anyActorRef, testActor, IntMessage(1))
    intAny = testMessagePattern(anyActorRef, testActor, { case IntMessage(_) ⇒ })
    btrue = testMessage(anyActorRef, testActor, BooleanMessage(true))
    bAny = testMessagePattern(anyActorRef, testActor, { case BooleanMessage(_) ⇒ })
  }

  "The Int and Bool messages" should "be processed" in {
    testActor ! IntMessage(5)
    testActor ! IntMessage(1)
    testActor ! BooleanMessage(false)
    testActor ! BooleanMessage(false)

    whenStable {
      //processingCount(bAny) should be(2)
      isDelivered(int1) should be(true)
      isProcessed(intAny) should be(true)
      deliveryCount(intAny) should be(2)
      processingCount(bAny) should be(2)
    }

  }

  override def tearDown {
    testActor.stop
  }

}