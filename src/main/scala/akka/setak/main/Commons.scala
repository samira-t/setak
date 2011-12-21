/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak

import akka.actor.Actor
import akka.actor.Actor._
import util.TestActorRefFactory
import akka.setak.core.TestMessageEnvelopSequence

/**
 * This actor is an actor whose instances can be matched with
 * any other actors.
 * It is a wild card for matching
 */
class AnyActor extends Actor {
  def receive = {
    case _ ⇒
  }
}

object Commons {
  /**
   * Wild card for the message content
   */
  val anyMessage = new Object()

  /**
   * Wild card for the sender or receiver of a test message
   */
  val anyActorRef = actorOf[AnyActor]

  /**
   * A pool of test factories for creating actors that can be controlled by each test.
   * For each test, a factory is created and added to this pool that can be used in different classes
   * to create actors that can be controlled by the test.
   */
  var testFactoryPool = new java.util.concurrent.ConcurrentLinkedQueue[TestActorRefFactory]()
}
