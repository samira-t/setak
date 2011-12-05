/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak.util
import akka.setak.core.TestExecutionManager
import akka.setak.core.TestMessageSequence
import akka.setak.core.TestSchedule
import akka.setak.core.TestActorRef
import akka.actor.Actor

/**
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 */

object TestExecutionUtil {

  /**
   * API for constraining the schedule of test execution and removing some non-determinism by specifying
   * a set of partial orders between the messages. The receivers of the messages in each partial order should
   * be the same (an instance of TestActorRef)
   */
  def setSchedule(partialOrders: Set[TestMessageSequence]) {
    /*
     * TODO: check if the receivers of all messages in each partial order are the same
     */
    for (po ← partialOrders) {
      if (po.head._receiver.isInstanceOf[TestActorRef]) {
        po.head._receiver.asInstanceOf[TestActorRef].addPartialOrderToSchedule(po)
      } else
        throw new Exception("The receiver of the test message in a schedule should be an instance of TestActorRef")
    }
  }

}