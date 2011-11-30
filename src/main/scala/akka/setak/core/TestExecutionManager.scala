/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak.core
import akka.actor.Actor
import akka.actor.ActorRef
import monitor.NotProcessedMessages
import monitor.AllDeliveredMessagesAreProcessed
import akka.actor.LocalActorRef
import scala.collection.mutable.HashSet
import scala.collection.mutable.ArrayBuffer
import akka.setak.TestConfig
import monitor._

/**
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 */
class TestExecutionManager(traceMonitorActor: ActorRef) {

  private var actorsWithMessages = new HashSet[ActorRef]

  def startTest {
  }

  /**
   * Checks for the stability of the system.
   * The system is stable if:
   * 1) the mailbox of the actors in the test are empty
   * 2) the monitor actor has received the confirmation of processing all the delivered messages
   * In fact, if condition 1 holds, the actor might be busy with processing the last message. The
   * second condition takes care of this situation
   *
   * It tries to check the stability of the system by giving maxTry. the default is 10 times
   * and each time it sleeps 100 milliseconds before trying again.
   *
   * @return false if the system did not get stable with that maxTry
   *
   *
   */
  def checkForStability(maxTry: Int = TestConfig.maxTryForStability): Boolean =
    {
      var triedCheck = 0
      var isStable = false
      var notProcessedMessages = new ArrayBuffer[RealMessageInvocation]()
      var monitor = false

      while (triedCheck < maxTry && !isStable) {
        triedCheck += 1
        Thread.sleep(TestConfig.sleepInterval * triedCheck)

        isStable = true
        monitor = false
        actorsWithMessages.clear()
        notProcessedMessages.clear()

        for (a ← Actor.registry) {
          if (a.isRunning) {
            if (a == traceMonitorActor && !a.asInstanceOf[LocalActorRef].dispatcher.mailboxIsEmpty(a.asInstanceOf[LocalActorRef])) {
              isStable = false
              if (triedCheck == maxTry) monitor = true
            } else if (a.isInstanceOf[TestActorRef]) {
              if (!a.asInstanceOf[TestActorRef].cloudIsEmpty || !a.asInstanceOf[TestActorRef].scheduleHappened ||
                !a.asInstanceOf[LocalActorRef].dispatcher.mailboxIsEmpty(a.asInstanceOf[LocalActorRef])) {

                isStable = false
                if (triedCheck == maxTry) {
                  println(a.asInstanceOf[TestActorRef].cloudIsEmpty + " " + a.asInstanceOf[LocalActorRef].dispatcher.mailboxSize(a.asInstanceOf[LocalActorRef]))
                }
              }
            }
          }
        }

        /*
         * If no message is in the mailbox of the actors check to see if they have finished with processing 
         * all of their messages. In that case, ask from monitor actor to see if the messages that 
         * have been delivered are processed.
         */
        if (isStable) {
          if (!(traceMonitorActor ? AllDeliveredMessagesAreProcessed).mapTo[Boolean].get) {
            isStable = false
            if (triedCheck == maxTry) {
              //for debugging: record the messages that are delivered but not processed yet
              notProcessedMessages = (traceMonitorActor ? NotProcessedMessages).mapTo[ArrayBuffer[RealMessageInvocation]].get
            }
            log("not all delivered messages are processed yet")

          }
        }
      }
      if (!isStable) { log("no stable" + monitor); if (notProcessedMessages.size > 0) log(notProcessedMessages.head._message.toString()) }
      isStable

    }

  /**
   * Stops each test by stopping all the test actors except the monitor actor
   */
  def stopTest {
    log("stability for stopping the test")
    // with a timeout, waits for the system to get stable 
    val stable = checkForStability()

    //Reset the current state of the traceMonitorActor to prepare it for the next test in the test case.
    (traceMonitorActor ? ClearState).get

    //Stop other actors including the monitor actor
    for (actor ← Actor.registry) {
      if (actor.isInstanceOf[TestActorRef] && actor.isRunning) { //&& actor != traceMonitorActor) { // stop other actors except the traceMonitorActor
        actor.stop
      }
    }

  }

  private var debug = false
  private def log(s: String) = if (debug) println(s)
}