///**
// * Copyright (C) 2009-2011 Typesafe Inc. <http://www.typesafe.com>
// */
//
//package akka.setack.test
//
//import org.scalatest.WordSpec
//import org.scalatest.matchers.MustMatchers
//
//import akka.setack.SetackWordSpec
//import akka.setack.Commons._
//
////import akka.testkit._
//
//import akka.actor.Actor
//import akka.actor._
//
//class HotSwapSpec extends SetackWordSpec with MustMatchers {
//
//  "An Actor" must {
//
//    //    "be able to hotswap its behavior with HotSwap(..)" in {
//    //      //val barrier = TestBarrier(2)
//    //      @volatile
//    //      var _log = ""
//    //      val a = actorOf(new Actor {
//    //        def receive = { case _ ⇒ _log += "default" }
//    //      })
//    //      a ! HotSwap(self ⇒ {
//    //        case _ ⇒
//    //          _log += "swapped"
//    //        //barrier.await
//    //      })
//    //      a ! "swapped"
//    //      //barrier.await
//    //
//    //      /* Added by Setack */
//    //      whenStable {
//    //        _log must be("swapped")
//    //
//    //      }
//    //    }
//
//    "be able to hotswap its behavior with become(..)" in {
//      //val barrier = TestBarrier(2)
//      //@volatile
//      //var _log = ""
//      val a = actorOf(new Actor {
//        def receive = {
//          case "init" ⇒
//          //_log += "init"
//          //barrier.await
//          case "swap" ⇒ become({
//            case _ ⇒
//            //_log += "swapped"
//            //barrier.await
//          })
//        }
//      })
//
//      var msg = testMessage(anyActorRef, a, "init")
//      a ! "init"
//      //barrier.await
//      //_log must be("init")
//      whenStable {
//        isProcessed(msg)
//      }
//
//      /* Added by Setack */
//      msg = testMessage(anyActorRef, a, "swapped")
//
//      //barrier.reset
//      //_log = ""
//      a ! "swap"
//      a ! "swapped"
//
//      //barrier.await
//      //_log must be("swapped")
//
//      /* Added by Setack */
//      whenStable {
//        isProcessed(msg)
//      }
//    }
//
//    "be able to revert hotswap its behavior with RevertHotSwap(..)" in {
//      //val barrier = TestBarrier(2)
//      //@volatile
//      //var _log = ""
//      val a = actorOf(new Actor {
//        def receive = {
//          case "init" ⇒
//          //_log += "init"
//          //barrier.await
//        }
//      })
//
//      /* Added by Setack */
//      val init = testMessage(anyActorRef, a, "init")
//      a ! "init"
//
//      //barrier.await
//      //_log must be("init")
//
//      /* Added by Setack */
//      whenStable {
//        isProcessed(init)
//      }
//
//      // barrier.reset
//      //_log = ""
//      a ! HotSwap(self ⇒ {
//        case "swapped" ⇒
//        //_log += "swapped"
//        //barrier.await
//      })
//
//      /* Added by Setack */
//      val swapped = testMessage(anyActorRef, a, "swapped")
//
//      a ! "swapped"
//      //barrier.await
//      //_log must be("swapped")
//
//      /* Added by Setack */
//      whenStable {
//        isProcessed(swapped)
//      }
//
//      //barrier.reset
//      //_log = ""
//      a ! RevertHotSwap
//
//      a ! "init"
//      //barrier.await
//      //_log must be("init")
//
//      /* Added by Setack */
//      whenStable {
//        processingCount(init) must be(2)
//      }
//
//      // try to revert hotswap below the bottom of the stack
//      //barrier.reset
//      //_log = ""
//      a ! RevertHotSwap
//
//      a ! "init"
//      //barrier.await
//      //_log must be("init")
//
//      /* Added by Setack */
//      whenStable {
//        processingCount(init) must be(3)
//      }
//    }
//
//    "be able to revert hotswap its behavior with unbecome" in {
//      // val barrier = TestBarrier(2)
//      // @volatile
//      // var _log = ""
//      val a = actorOf(new Actor {
//        def receive = {
//          case "init" ⇒
//          //_log += "init"
//          //barrier.await
//          case "swap" ⇒
//            become({
//              case "swapped" ⇒
//              //_log += "swapped"
//              //barrier.await
//              case "revert" ⇒
//                unbecome()
//            })
//          //barrier.await
//        }
//      })
//
//      /* Added by Setack */
//      val init = testMessage(anyActorRef, a, "init")
//
//      a ! "init"
//      // barrier.await
//      // _log must be("init")
//
//      /* Added by Setack */
//      whenStable {
//        isProcessed(init)
//      }
//
//      //barrier.reset
//      //_log = ""
//      a ! "swap"
//      //barrier.await
//
//      //barrier.reset
//      //_log = ""
//
//      /* Added by Setack */
//      val swapped = testMessage(anyActorRef, a, "swapped")
//
//      a ! "swapped"
//      //barrier.await
//      //_log must be("swapped")
//
//      /* Added by Setack */
//      whenStable {
//        isProcessed(swapped)
//      }
//
//      //barrier.reset
//      //_log = ""
//      a ! "revert"
//      a ! "init"
//      // barrier.await
//      // _log must be("init")
//
//      /* Added by Setack */
//      whenStable {
//        processingCount(init) must be(2)
//      }
//
//    }
//  }
//}
