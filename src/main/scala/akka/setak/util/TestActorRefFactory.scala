/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak.util
import akka.actor.LocalActorRef
import akka.actor.Actor
import akka.japi.Creator
import akka.actor.ActorInitializationException
import akka.util.ReflectiveAccess
import akka.event.EventHandler
import java.lang.reflect.InvocationTargetException
import com.eaio.uuid.UUID
import akka.actor.ActorRef
import akka.setak.core.TestActorRef

/**
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 *
 * The factory methods in Actor object should be replaced with the factory methods in this
 * object. It returns a TestActorRef as the reference instead of ActroRef
 */
class TestActorRefFactory(traceMonitorActor: ActorRef) {

  /**
   *  Creates a TestActorRef out of the Actor with type T.
   * <pre>
   *   import TestActorRefFactory._
   *   val actor = actorOf[MyActor]
   *   actor.start()
   *   actor ! message
   *   actor.stop()
   * </pre>
   * You can create and start the actor in one statement like this:
   * <pre>
   *   val actor = actorOf[MyActor].start()
   * </pre>
   */
  def actorOf[T <: Actor: Manifest]: TestActorRef = actorOf(manifest[T].erasure.asInstanceOf[Class[_ <: Actor]])

  /**
   * Creates a TestActorRef out of the Actor of the specified Class.
   * <pre>
   *   import TestActorRefFactory._
   *   val actor = actorOf(classOf[MyActor])
   *   actor.start()
   *   actor ! message
   *   actor.stop()
   * </pre>
   * You can create and start the actor in one statement like this:
   * <pre>
   *   val actor = actorOf(classOf[MyActor]).start()
   * </pre>
   */
  def actorOf(clazz: Class[_ <: Actor]): TestActorRef = new TestActorRef(() ⇒ {
    import ReflectiveAccess.{ createInstance, noParams, noArgs }
    createInstance[Actor](clazz.asInstanceOf[Class[_]], noParams, noArgs) match {
      case Right(actor) ⇒ actor
      case Left(exception) ⇒
        val cause = exception match {
          case i: InvocationTargetException ⇒ i.getTargetException
          case _                            ⇒ exception
        }

        throw new ActorInitializationException(
          "Could not instantiate Actor of " + clazz +
            "\nMake sure Actor is NOT defined inside a class/trait," +
            "\nif so put it outside the class/trait, f.e. in a companion object," +
            "\nOR try to change: 'actorOf[MyActor]' to 'actorOf(new MyActor)'.", cause)
    }

  }, None, traceMonitorActor)

  /**
   * Creates a TestActorRef out of the Actor. Allows you to pass in a factory function
   * that creates the Actor. Please note that this function can be invoked multiple
   * times if for example the Actor is supervised and needs to be restarted.
   * <p/>
   * <pre>
   *   import TestActorRefFactory._
   *   val actor = actorOf(new MyActor)
   *   actor.start()
   *   actor ! message
   *   actor.stop()
   * </pre>
   * You can create and start the actor in one statement like this:
   * <pre>
   *   val actor = actorOf(new MyActor).start()
   * </pre>
   */
  def actorOf(factory: ⇒ Actor): TestActorRef = new TestActorRef(() ⇒ factory, None, traceMonitorActor)

}