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
import akka.setak.SetakTest

/**
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 *
 * The factory methods in Actor object should be replaced with the factory methods in this
 * object. It returns a TestActorRef as the reference instead of ActroRef.
 *
 * For each test, a TestActorRefFactory is created that can be used for creating actors as instance of TestActroRef.
 * This factory is passed to each created (test) actor so that they can use that for creating child actors, i.e. dynamic creation of actors.
 */
class TestActorRefFactory(test: SetakTest) {

  /**
   *  Creates a TestActorRef out of the Actor with type T.
   * <pre>
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
  def actorOf(clazz: Class[_ <: Actor]): TestActorRef = new TestActorRef(test.testActorRefFactory, test.anonymousSchedule, () ⇒ {
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

  }, None, test.traceMonitorActor)

  /**
   * Creates a TestActorRef out of the Actor. Allows you to pass in a factory function
   * that creates the Actor. Please note that this function can be invoked multiple
   * times if for example the Actor is supervised and needs to be restarted.
   *
   * TestActroRefFactory passes itself as an argument to the TestActorRef so that in the
   * case that actors are created dynamically inside of the actors (outside of the test class), they use the
   * same TestActorRefFactory.
   *
   * <p/>
   * <pre>
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
  def actorOf(factory: ⇒ Actor): TestActorRef = new TestActorRef(test.testActorRefFactory, test.anonymousSchedule, () ⇒ factory, None, test.traceMonitorActor)

}