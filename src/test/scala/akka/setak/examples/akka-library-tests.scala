/**
 * Copyright (C) 2011 Samira Tasharofi
 *
 * This test is taken from Akka library test suite:
 * https://github.com/jboner/akka/blob/release-1.2/akka-actor-tests/src/test/scala/akka/actor/actor/ActorFireForgetRequestReplySpec.scala.
 * It is translated to Setak test to show how the test cases can be simplified using Setak.
 *
 * The commented lines show the parts that are eliminated from the
 * original test case by using Setak.
 * Specifically, it shows the removal of latches and other synchronization
 * constructs in writing test cases.
 */

package akka.setak.examples

//import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
//import org.scalatest.BeforeAndAfterEach

//import akka.testkit._
import akka.setak._
//import akka.testkit.Testing.sleepFor
//import akka.util.duration._

import akka.actor.Actor
import akka.config.Supervision._
import akka.dispatch.Dispatchers
import akka.actor.ActorRef

import akka.setak.SetakWordSpec
import akka.setak.Commons._

object ActorFireForgetRequestReplySpec {

  class ReplyActor extends Actor {
    def receive = {
      case "Send" ⇒
        self.reply("Reply")
      case "SendImplicit" ⇒
        self.channel ! "ReplyImplicit"
    }
  }

  class CrashingTemporaryActor extends Actor {
    self.lifeCycle = Temporary
    def receive = {
      case "Die" ⇒
        //state.finished.await
        throw new Exception("Expected exception")
    }
  }

  class SenderActor(replyActor: ActorRef) extends Actor {
    def receive = {
      case "Init" ⇒
        replyActor ! "Send"
      case "Reply" ⇒ {
        //state.s = "Reply"
        //state.finished.await
      }
      case "InitImplicit" ⇒ replyActor ! "SendImplicit"
      case "ReplyImplicit" ⇒ {
        //state.s = "ReplyImplicit"
        //state.finished.await
      }
    }
  }

  //  object state {
  //    var s = "NIL"
  //    val finished = TestBarrier(2)
  //  }
}

class ActorFireForgetRequestReplySpec extends SetakWordSpec with MustMatchers { //with BeforeAndAfterEach {
  import ActorFireForgetRequestReplySpec._
  //  override def beforeEach() = {
  //    state.finished.reset
  //  }

  "An Actor" must {

    "reply to bang message using reply" in {
      val replyActor = actorOf[ReplyActor].start()
      val senderActor = actorOf(new SenderActor(replyActor)).start()

      /* Added by Setak*/
      val replyMsg = testMessageEnvelop(anyActorRef, senderActor, "Reply")

      senderActor ! "Init"

      //state.finished.await
      //state.s must be("Reply")

      /* Added by Setak*/
      whenStable {
        isProcessed(replyMsg) must be(true)
      }
    }

    "reply to bang message using implicit sender" in {
      val replyActor = actorOf[ReplyActor].start()
      val senderActor = actorOf(new SenderActor(replyActor)).start()

      /* Added by Setak*/
      val replyImplicit = testMessageEnvelop(anyActorRef, senderActor, "ReplyImplicit")

      senderActor ! "InitImplicit"

      //state.finished.await
      //state.s must be("ReplyImplicit")

      /* Added by Setak*/
      whenStable {
        isProcessed(replyImplicit) must be(true)
      }
    }

    "should shutdown crashed temporary actor" in {
      val actor = actorOf[CrashingTemporaryActor].start()
      actor.isRunning must be(true)
      val die = testMessageEnvelop(anyActorRef, actor, "Die")
      actor ! "Die"

      //state.finished.await
      //sleepFor(1 second)

      /* Added by Setak*/
      afterMessages(die) {
        actor.isShutdown must be(true)

      }
    }
  }
}
