/**
 * Copyright (C) 2011 Samira Tasharofi
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
import akka.setack.Commons._
import akka.setack._
import scala.collection.mutable.ListBuffer

class SampleActor(var brother: ActorRef = null) extends Actor {
  var messageOrder = ListBuffer[Any]()
  def receive = {
    case msg @ ('m)  ⇒ messageOrder.+=(msg)
    case msg @ 'req  ⇒ messageOrder.+=(msg); if (brother != null) { val f = brother ? 'req2; f.get }
    case msg @ 'req2 ⇒ messageOrder.+=(msg); self.reply('reply)

  }
}

class TestFutureMeesages extends SetackJUnit with org.scalatest.junit.JUnitSuite {

  var a: TestActorRef = null
  var b: TestActorRef = null
  var m: TestMessageInvocation = null
  var req2: TestMessageInvocation = null
  var reply: TestMessageInvocation = null

  @Before
  def setUp {
    a = actorOf(new SampleActor()).start
    b = actorOf(new SampleActor(a)).start
    m = testMessage(anyActorRef, a, 'm)
    req2 = testMessage(anyActorRef, a, 'req2)
    reply = testMessage(a, anyActorRef, 'reply)
  }

  @Test
  def testDeliveryToFuture {
    b ! 'req

    whenStable {
      assert(isProcessed(req2))
      assert(isDelivered(reply))
    }

  }

  @Test
  def testScheduleFutureAndActorMessages {
    setSchedule(req2 -> m)
    a ! 'm
    b ! 'req

    whenStable {
      assert(isProcessed(req2))
      assert(isDelivered(reply))
      assert(a.actorObject[SampleActor].messageOrder.indexOf('req2) < a.actorObject[SampleActor].messageOrder.indexOf('m))
    }
  }

  @After
  def tearDown {
  }

}