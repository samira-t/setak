/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak.core

import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

/**
 * The schedule is a set of partial orders between the test messages.
 * This class is synchronized to make it thread-safe.
 *
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 *
 */
class TestSchedule {

  private var testMessageEnvelopSequences = new HashSet[TestMessageEnvelopSequence]

  def this(schedule: Set[TestMessageEnvelopSequence]) {
    this()
    for (testMessageEnvelopSequence ← schedule)
      testMessageEnvelopSequences.+=(testMessageEnvelopSequence)
  }

  def addPartialOrder(po: TestMessageEnvelopSequence) = synchronized {
    testMessageEnvelopSequences.+=(po)
  }

  /**
   * @return The least index of the message among all the sequences.
   * There are multiple cases for the returned index:
   * 1) the index is zero: the message can be delivered
   * 2) the index is greater than zero: the message should be delivered later
   * 3) the index is -1: the message can be delivered without any constraints
   * (the message is not matched with any messages in the partial orders)
   */
  def leastIndexOf(messageEnvelop: RealMessageEnvelop): Int = synchronized {
    var leastIndex = -1
    for (testMessageEnvelopSequence ← testMessageEnvelopSequences) {
      val currIndex = testMessageEnvelopSequence.indexOf(messageEnvelop)
      if (currIndex == 0) return currIndex
      else if (currIndex > 0 && leastIndex == -1)
        leastIndex = currIndex
      else if (currIndex > 0 && leastIndex > -1)
        leastIndex = math.min(currIndex, leastIndex)
    }
    return leastIndex

  }
  /**
   * It is called by the dispatcher to remove the message that is in the head
   * of partial orders in the schedule (move forward the pointer for the current
   * schedule).
   */
  def removeFromHead(messageEnvelop: RealMessageEnvelop): Boolean = synchronized {
    for (testMessageEnvelopSequence ← testMessageEnvelopSequences) {
      if (testMessageEnvelopSequence.head != null &&
        testMessageEnvelopSequence.head.matchWithRealEnvelop(messageEnvelop)) {
        val result = testMessageEnvelopSequence.removeHead
        if (testMessageEnvelopSequence.isEmpty)
          testMessageEnvelopSequences.remove(testMessageEnvelopSequence)
        return result
      }
    }
    return false

  }

  def isEmpty = synchronized {
    testMessageEnvelopSequences.isEmpty
  }

  override def toString(): String = synchronized {
    var outString = "schedule = "
    for (sequnece ← testMessageEnvelopSequences) {
      outString += sequnece.messageSequence.mkString("->")
      outString += ", "
    }
    outString
  }

}