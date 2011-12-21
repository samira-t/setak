/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak.test
import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef
import akka.setak.core.TestMessageEnvelop
import akka.setak.core.TestMessageEnvelopSequence._
import akka.setak.core.TestActorRef
import akka.setak.Commons._
import akka.setak._

/**
 * This test checks that the actor factory in the test is appropriately passed to each actor under test and they can use that factory
 * for creating child actors. Therefore, all the dynamic created actors in a test would be under the control of the same test.
 */
class MasterActor extends Actor {

  def receive = {
    case 'createUsingSelfTestFactory ⇒ {
      val child = self.asInstanceOf[TestActorRef].testActorRefFactory.actorOf(new Actor { def receive = { case _ ⇒ } })
      self.reply(child)
    }
    case 'createUsingCommonPoolTestFactory ⇒ {
      val child = akka.setak.Commons.testFactoryPool.peek().actorOf(new Actor { def receive = { case _ ⇒ } })
      self.reply(child)
    }
    case 'createUsingAkkaFactory ⇒ {
      val child = actorOf(new Actor { def receive = { case _ ⇒ } })
      self.reply(child)
    }
  }
}
class TestDynamicActorCreation extends SetakFlatSpec {

  "The child actor created by self factory" should
    "be an insatnce of TestActroRef" in {

      val master = actorOf[MasterActor].start
      assert((master ? 'createUsingSelfTestFactory).get.isInstanceOf[TestActorRef])

    }
  "The child actor created by common pool factory" should
    "be an insatnce of TestActroRef" in {

      val master = actorOf[MasterActor].start
      assert((master ? 'createUsingCommonPoolTestFactory).get.isInstanceOf[TestActorRef])

    }

  "The child actor" should " not be an insatnce of TestActroRef" in {

    val master = actorOf[MasterActor].start
    assert(!((master ? 'createUsingAkkaFactory).get.isInstanceOf[TestActorRef]))

  }
}