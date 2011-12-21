package com.weiglewilczek.demo.akka
package banking

import Account._
import Bank._
//import org.scalatest.WordSpec
import akka.setak._
import org.scalatest.matchers.MustMatchers._

import akka.actor.Actor._

/**
 *
 * This test is taken from Akka examples (Banking example).
 * It is translated to Setak test to show how whenStable can be used for eliminating Thread sleep from the test cases.
 */

class BankSpec extends SetakWordSpec {

  "Calling Bank.transfer with an amount of 10" should {
    """result in an amount of -10 for the "from" Account and an amount of 10 for the "to" Account.""" in {
      val bank = actorOf[Bank].start
      val from = actorOf[Account].start
      val to = actorOf[Account].start
      val withdraw = testMessageEnvelop(bank, from, Withdraw(10))
      val deposit = testMessageEnvelop(bank, to, Deposit(10))
      try {
        bank ! Transfer(10, from, to)

        /*
         * Eliminated by Setak. Setak does not need "Thread sleep" since whenStable would perform 
         * the funtionality of letting the actors finish processing their messages. 
         */
        //Thread sleep 1000 // Let's wait until the transfer is finished!

        afterAllMessages {
          val fromBalance = from !! GetBalance
          fromBalance must equal(Some(Balance(-10)))
          val toBalance = to !! GetBalance
          toBalance must equal(Some(Balance(10)))
        }
      } finally {
        from.stop
        to.stop
        bank.stop
      }
    }
  }

  //  "Calling Bank.transfer with an amount of 20" should {
  //    """result in an amount of 0 for the "from" Account and an amount of 0 for the "to" Account.""" in {
  //      val bank = actorOf[Bank].start
  //      val from = actorOf[Account].start
  //      val to = actorOf[Account].start
  //      try {
  //        bank ! Transfer(20, from, to)
  //
  //        Thread sleep 1000 // Let's wait until the transfer is finished!
  //        //whenStable {
  //
  //        val fromBalance = from !! GetBalance
  //        fromBalance must equal(Some(Balance(0)))
  //        val toBalance = to !! GetBalance
  //        toBalance must equal(Some(Balance(0)))
  //        //}
  //      } finally {
  //        from.stop
  //        to.stop
  //        bank.stop
  //      }
  //    }
  //  }
}
