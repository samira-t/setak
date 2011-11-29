//package akka.setack.test
//
//import org.scalatest.WordSpec
//import org.scalatest.matchers.MustMatchers
//import akka.routing._
//import java.util.concurrent.atomic.AtomicInteger
////import akka.actor.Actor._
//import akka.actor.{ ActorRef, Actor }
//import collection.mutable.LinkedList
//import akka.routing.Routing.Broadcast
//import java.util.concurrent.{ CountDownLatch, TimeUnit }
//import akka.setack.core.TestMessageInvocation
//import akka.setack.core.TestActorRef
//import akka.setack.SetackWordSpec
//import akka.setack.Commons._
//
//object RoutingSpec {
//
//  class TestActor extends Actor with Serializable {
//    def receive = {
//      case _ ⇒
//        println("Hello")
//    }
//  }
//}
//
//class RoutingSpec extends SetackWordSpec with MustMatchers {
//
//  import akka.setack.test.RoutingSpec._
//
//  "direct router" must {
//    "be started when constructed" in {
//      val actor1 = Actor.actorOf[TestActor]
//
//      val actor = Routing.actorOf("foo", List(actor1), RouterType.Direct)
//      actor.isRunning must be(true)
//    }
//
//    "throw IllegalArgumentException at construction when no connections" in {
//      try {
//        Routing.actorOf("foo", List(), RouterType.Direct)
//        fail()
//      } catch {
//        case e: IllegalArgumentException ⇒
//      }
//    }
//
//    "send message to connection" in {
//      //val doneLatch = new CountDownLatch(1)
//
//      //val counter = new AtomicInteger(0)
//      val connection1 = actorOf(new Actor {
//        def receive = {
//          case "end" ⇒ //doneLatch.countDown()
//          case _     ⇒ //counter.incrementAndGet
//        }
//      })
//
//      val end = testMessage(anyActorRef, connection1, "end")
//      val other = testMessage(anyActorRef, connection1, "hello")
//      val routedActor = Routing.actorOf("foo", List(connection1), RouterType.Direct)
//      routedActor ! "hello"
//      routedActor ! "end"
//
//      //doneLatch.await(5, TimeUnit.SECONDS) must be(true)
//      //counter.get must be(1)
//
//      whenStable {
//        processingCount(other) must be(1)
//        processingCount(end) must be(1)
//      }
//    }
//
//    "deliver a broadcast message" in {
//      //val doneLatch = new CountDownLatch(1)
//
//      //val counter1 = new AtomicInteger
//      val connection1 = actorOf(new Actor {
//        def receive = {
//          case "end"    ⇒ //doneLatch.countDown()
//          case msg: Int ⇒ //counter1.addAndGet(msg)
//        }
//      })
//
//      val end = testMessage(anyActorRef, connection1, "end")
//      val int = testMessagePattern(anyActorRef, connection1, { case msg: Int ⇒ })
//
//      val actor = Routing.actorOf("foo", List(connection1), RouterType.Direct)
//
//      actor ! Broadcast(1)
//      actor ! "end"
//
//      //doneLatch.await(5, TimeUnit.SECONDS) must be(true)
//
//      //counter1.get must be(1)
//      whenStable {
//        processingCount(end) must be(1)
//        processingCount(int) must be(1)
//      }
//
//    }
//  }
//
//  "round robin router" must {
//
//    "be started when constructed" in {
//      val actor1 = Actor.actorOf[TestActor]
//
//      val actor = Routing.actorOf("foo", List(actor1), RouterType.RoundRobin)
//      actor.isRunning must be(true)
//    }
//
//    "throw IllegalArgumentException at construction when no connections" in {
//      try {
//        Routing.actorOf("foo", List(), RouterType.RoundRobin)
//        fail()
//      } catch {
//        case e: IllegalArgumentException ⇒
//      }
//    }
//
//    //In this test a bunch of actors are created and each actor has its own counter.
//    //to test round robin, the routed actor receives the following sequence of messages 1 2 3 .. 1 2 3 .. 1 2 3 which it
//    //uses to increment his counter.
//    //So after n iteration, the first actor his counter should be 1*n, the second 2*n etc etc.
//    "deliver messages in a round robin fashion" in {
//      val connectionCount = 10
//      val iterationCount = 10
//      //val doneLatch = new CountDownLatch(connectionCount)
//
//      //lets create some connections.
//      var connections = new LinkedList[ActorRef]
//      var counters = new LinkedList[AtomicInteger]
//      for (i ← 0 until connectionCount) {
//        counters = counters :+ new AtomicInteger()
//
//        val connection = actorOf(new Actor {
//          def receive = {
//            //case "end"    ⇒ doneLatch.countDown()
//            case msg: Int ⇒ counters.get(i).get.addAndGet(msg)
//          }
//        })
//        connections = connections :+ connection
//      }
//
//      //create the routed actor.
//      val actor = Routing.actorOf("foo", connections, RouterType.RoundRobin)
//
//      //send messages to the actor.
//      for (i ← 0 until iterationCount) {
//        for (k ← 0 until connectionCount) {
//          actor ! (k + 1)
//        }
//      }
//
//      //actor ! Broadcast("end")
//      //now wait some and do validations.
//      //doneLatch.await(5, TimeUnit.SECONDS) must be(true)
//
//      whenStable {
//        for (i ← 0 until connectionCount) {
//          val counter = counters.get(i).get
//          counter.get must be((iterationCount * (i + 1)))
//        }
//      }
//    }
//
//    "deliver a broadcast message using the !" in {
//      //val doneLatch = new CountDownLatch(2)
//
//      //val counter1 = new AtomicInteger
//
//      //          val connection1 = actorOf(new Actor {
//      //            def receive = {
//      //              case "end"    ⇒ //doneLatch.countDown()
//      //              case msg: Int ⇒ //counter1.addAndGet(msg)
//      //            }
//      //          })
//
//      //val counter2 = new AtomicInteger
//      //          val connection2 = actorOf(new Actor {
//      //            def receive = {
//      //              case "end"    ⇒ //doneLatch.countDown()
//      //              case msg: Int ⇒ //counter2.addAndGet(msg)
//      //            }
//      //          })
//
//      class Connection extends Actor {
//        def receive = {
//          case "end"    ⇒
//          case msg: Int ⇒
//        }
//      }
//      val connection1 = actorOf(new Connection())
//      val connection2 = actorOf(new Connection())
//
//      val end = testMessage(anyActorRef, anyActorRef, "end")
//      val int1 = testMessage(anyActorRef, connection1, 1)
//      val int2 = testMessage(anyActorRef, connection2, 1)
//
//      val actor = Routing.actorOf("foo", List(connection1, connection2), RouterType.RoundRobin)
//
//      actor ! Broadcast(1)
//      actor ! Broadcast("end")
//
//      //doneLatch.await(5, TimeUnit.SECONDS) must be(true)
//
//      //counter1.get must be(1)
//      //counter2.get must be(1)
//
//      whenStable {
//        processingCount(end) must be(2)
//        processingCount(int1) must be(1)
//        processingCount(int2) must be(1)
//      }
//    }
//
//    "fail to deliver a broadcast message using the ?" in {
//      //val doneLatch = new CountDownLatch(1)
//
//      //val counter1 = new AtomicInteger
//      val connection1 = actorOf(new Actor {
//        def receive = {
//          case "end" ⇒ //doneLatch.countDown()
//          case _     ⇒ //counter1.incrementAndGet()
//        }
//      })
//
//      //Added by Setack 
//      val end = testMessage(anyActorRef, connection1, "end")
//      val int1 = testMessage(anyActorRef, connection1, 1)
//
//      val actor = Routing.actorOf("foo", List(connection1), RouterType.RoundRobin)
//
//      try {
//        actor ? Broadcast(1)
//        fail()
//      } catch {
//        case e: RoutingException ⇒
//      }
//
//      actor ! "end"
//      //doneLatch.await(5, TimeUnit.SECONDS) must be(true)
//      //counter1.get must be(0)
//
//      //Added by Setack 
//      whenStable {
//        processingCount(end) must be(1)
//        deliveryCount(int1) must be(0)
//      }
//    }
//  }
//
//  "random router" must {
//
//    "be started when constructed" in {
//
//      val actor1 = Actor.actorOf[TestActor]
//
//      val actor = Routing.actorOf("foo", List(actor1), RouterType.Random)
//      actor.isRunning must be(true)
//    }
//
//    "throw IllegalArgumentException at construction when no connections" in {
//      try {
//        Routing.actorOf("foo", List(), RouterType.Random)
//        fail()
//      } catch {
//        case e: IllegalArgumentException ⇒
//      }
//    }
//
//    "deliver messages in a random fashion" in {
//
//    }
//
//    "deliver a broadcast message" in {
//      //val doneLatch = new CountDownLatch(2)
//
//      //val counter1 = new AtomicInteger
//      val connection1 = actorOf(new Actor {
//        def receive = {
//          case "end"    ⇒ //doneLatch.countDown()
//          case msg: Int ⇒ //counter1.addAndGet(msg)
//        }
//      })
//
//      //val counter2 = new AtomicInteger
//      val connection2 = actorOf(new Actor {
//        def receive = {
//          case "end"    ⇒ //doneLatch.countDown()
//          case msg: Int ⇒ //counter2.addAndGet(msg)
//        }
//      })
//
//      //Added by Setack 
//      val int1 = testMessage(anyActorRef, connection1, 1)
//      val end1 = testMessage(anyActorRef, connection1, "end")
//      val int2 = testMessage(anyActorRef, connection2, 1)
//      val end2 = testMessage(anyActorRef, connection2, "end")
//
//      val actor = Routing.actorOf("foo", List(connection1, connection2), RouterType.Random)
//
//      actor ! Broadcast(1)
//      actor ! Broadcast("end")
//
//      //doneLatch.await(5, TimeUnit.SECONDS) must be(true)
//
//      //counter1.get must be(1)
//      //counter2.get must be(1)
//
//      //Added by Setack 
//      whenStable {
//        isProcessed(end1) must be(true)
//        processingCount(int1) must be(1)
//        isProcessed(end2) must be(true)
//        processingCount(int2) must be(1)
//      }
//    }
//
//    "fail to deliver a broadcast message using the ?" in {
//      //val doneLatch = new CountDownLatch(1)
//
//      //val counter1 = new AtomicInteger
//      val connection1 = actorOf(new Actor {
//        def receive = {
//          case "end" ⇒ //doneLatch.countDown()
//          case _     ⇒ //counter1.incrementAndGet()
//        }
//      })
//
//      //Added by Setack 
//      val int = testMessage(anyActorRef, connection1, 1)
//
//      val actor = Routing.actorOf("foo", List(connection1), RouterType.Random)
//
//      try {
//        actor ? Broadcast(1)
//        fail()
//      } catch {
//        case e: RoutingException ⇒
//      }
//
//      actor ! "end"
//      //doneLatch.await(5, TimeUnit.SECONDS) must be(true)
//      //counter1.get must be(0)
//
//      //Added by Setack 
//      whenStable {
//        isProcessed(int) must be(false)
//      }
//
//    }
//  }
//
//  "least cpu router" must {
//    "throw IllegalArgumentException when constructed" in {
//      val actor1 = Actor.actorOf[TestActor]
//
//      try {
//        Routing.actorOf("foo", List(actor1), RouterType.LeastCPU)
//      } catch {
//        case e: IllegalArgumentException ⇒
//      }
//    }
//  }
//
//  "least ram router" must {
//    "throw IllegalArgumentException when constructed" in {
//      val actor1 = Actor.actorOf[TestActor]
//
//      try {
//        Routing.actorOf("foo", List(actor1), RouterType.LeastRAM)
//      } catch {
//        case e: IllegalArgumentException ⇒
//      }
//    }
//  }
//
//  "smallest mailbox" must {
//    "throw IllegalArgumentException when constructed" in {
//      val actor1 = Actor.actorOf[TestActor]
//
//      try {
//        Routing.actorOf("foo", List(actor1), RouterType.LeastMessages)
//      } catch {
//        case e: IllegalArgumentException ⇒
//      }
//    }
//  }
//}
