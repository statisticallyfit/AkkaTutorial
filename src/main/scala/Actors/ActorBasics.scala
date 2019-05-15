package Actors


import akka.actor._
import akka.util.Timeout

//futures
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try //places default thread pool in scope so Future can be
// executed asynchronously.
import scala.concurrent.{Await, Future, Promise, future}
import scala.concurrent.duration._
import scala.util.Random
import scala.util.{Success, Failure}


/**
  * http://allaboutscala.com/scala-frameworks/akka/#what-is-akka
  */




object IntroActorSystem extends App {

	//create actor system
	val system = ActorSystem("DonutStoreActorSystem")
	//close it
	val isTerminated = system.terminate()

	// register a future oncomplete() callback to show the system was
	//successfully shut down
	isTerminated.onComplete {
		case Success(result) => println("Successfully terminated actor system")
		case Failure(e) => println("Failed to terminate")
	}

	Thread.sleep(5000)
}



object TellPattern extends App {
	//Goal: create akka actor and design message passing protocol
	//Tell pattern: send a message to actor but expect no response (fire and forget)
	val system = ActorSystem("DonutStoreActorSystem")

	//create messgae passing protocol
	object DonutStoreProtocol {
		case class Info(name: String)
	}
	import DonutStoreProtocol._

	//create actor: make a class extending the Actor trait
	//receive method: where you can instruct your actor which messages it is designed
	//to react to
	class DonutInfoActor extends Actor with ActorLogging {

		def receive ={
			case Info(name) => log.info(s"Found $name donut") //the reaction of the actor
		}
	}

	//actually creating the actor
	val donutInfoActor: ActorRef = system.actorOf(Props[DonutInfoActor], name="DonutInfoActor")

	//send a message using the tell pattern
	//use the ! (bang) operator
	donutInfoActor ! Info("vanilla")

	//close the system
	val isTerminated = system.terminate()
	isTerminated.onComplete {
		case Success(result) => println("Successfully terminated actor system")
		case Failure(e) => println("Failed to terminate")
	}

	Thread.sleep(5000)
}



object AskPattern extends App {

	import akka.pattern.ask //for the ? operator
	//Ask pattern: allows you to send message to an actor and get a response back.
	// Tell pattern: no response

	//Create actor system
	println("\nStep 1: create actor system")//------------------------------------------------
	val system = ActorSystem("DonutStoreActorSystem")

	//create messgae passing protocol: this class represents the message sent
	println("\nStep 2: define the message passing protocol for the actor")//------------------------------------------------

	object DonutStoreProtocol {
		case class Info(name: String)
	}
	import DonutStoreProtocol._

	//create actor: make a class extending the Actor trait
	//receive method: where you can instruct your actor which messages it is designed
	//to react to
	//senter method: need to implement: specifies how to reply back to the source where
	// the message originated
	println("\nStep 3: Create DonutInfoActor")//------------------------------------------------

	class DonutInfoActor extends Actor with ActorLogging {

		//responding by logging that we found the donut
		//replying: by  returning true for when info message is vanilla, false otherwise
		def receive ={

			case Info(name) if name == "vanilla" => {
				log.info(s"Found VALID $name donut")
				sender ! true
			} //the reaction of the actor

			case Info(name) => {
				log.info(s"$name donut not supported")
				sender ! false
			}
		}
	}

	//actually creating the actor
	val donutInfoActor: ActorRef = system.actorOf(Props[DonutInfoActor], name="DonutInfoActor")


	//send a message using the ask pattern//use the ? operator
	// Ask pattern will return a future. We use for loop to print response from DonutInfoActor
	println("\nStep 4: Akka Ask Pattern")//------------------------------------------------

	implicit val timeout = Timeout(5 second)
	val vanillaDonutFound = donutInfoActor ? Info("vanilla")
	for {
		found <- vanillaDonutFound
	} yield (println(s"Vanilla donut found = $found"))

	val glazedDonutFound = donutInfoActor ? Info("glazed")
	for {
		found <- glazedDonutFound
	} yield (println(s"Glazed donut found = $found"))


	//close the system
	println("\nStep 5: Close the actor system")//------------------------------------------------

	val isTerminated = system.terminate()
	isTerminated.onComplete {
		case Success(result) => println("Successfully terminated actor system")
		case Failure(e) => println("Failed to terminate")
	}

	Thread.sleep(5000)
}



object AskPattern_mapTo extends App {
	import akka.pattern.ask //for the ? operator
	//Future.mapTo() method: can be used to provide a type to the future returned by
	// the Ask pattern

	//Create actor system
	println("\nStep 1: create actor system")//------------------------------------------------
	val system = ActorSystem("DonutStoreActorSystem")

	//create message passing protocol: this class represents the message sent
	println("\nStep 2: define the message passing protocol for the actor")//------------------------------------------------

	object DonutStoreProtocol {
		case class Info(name: String)
	}
	import DonutStoreProtocol._

	//create actor: make a class extending the Actor trait
	//receive method: where you can instruct your actor which messages it is designed
	//to react to
	//senter method: need to implement: specifies how to reply back to the source where
	// the message originated
	println("\nStep 3: Create DonutInfoActor")//------------------------------------------------

	class DonutInfoActor extends Actor with ActorLogging {

		//responding by logging that we found the donut
		//replying: by  returning true for when info message is vanilla, false otherwise
		def receive ={

			case Info(name) if name == "vanilla" => {
				log.info(s"Found VALID $name donut")
				sender ! true
			} //the reaction of the actor

			case Info(name) => {
				log.info(s"$name donut not supported")
				sender ! false
			}
		}
	}

	//actually creating the actor
	val donutInfoActor: ActorRef = system.actorOf(Props[DonutInfoActor], name="DonutInfoActor")


	//send a message using the ask pattern//use the ? operator
	// Ask pattern will return a future. We use for loop to print response from DonutInfoActor
	println("\nStep 4: Akka Ask Pattern and future mapTo()")//------------------------------------------------

	implicit val timeout = Timeout(5 second)

	//val vanillaDonutFound: Future[Any] = donutInfoActor ? Info("vanilla")
	//note: instead of Future[Any] we can have Future[Boolean]
	val vanillaDonutFound: Future[Boolean] = (donutInfoActor ? Info("vanilla")).mapTo[Boolean]
	for {
		found <- vanillaDonutFound
	} yield (println(s"Vanilla donut found = $found"))

	val glazedDonutFound = donutInfoActor ? Info("glazed")
	for {
		found <- glazedDonutFound
	} yield (println(s"Glazed donut found = $found"))


	//close the system
	println("\nStep 5: Close the actor system")//------------------------------------------------

	val isTerminated = system.terminate()
	Thread.sleep(5000)


}





object AskPattern_pipeTo extends App {
	import akka.pattern.ask //for the ? operator
	import akka.pattern.pipe //for the pipeTo


	//Future.pipeTo() method: attaches to a future operation by registering hte Future andThen
	//callback to let us easily send result back to sender

	//Create actor system//------------------------------------------------
	println("\nStep 1: create actor system")
	val system = ActorSystem("DonutStoreActorSystem")

	//create message passing protocol: this class represents the message sent
	println("\nStep 2: define the message passing protocol for the actor")//------------------------------------------------

	object DonutStoreProtocol {
		case class Info(name: String)
		//augment the message passing protocl
		//CheckStock would check the stock qunantity for a given donut
		case class CheckStock(name: String)
	}
	import DonutStoreProtocol._

	//create actor: make a class extending the Actor trait
	//DonutStockActor REACTS to a CheckStock message and delegates the call to a future
	// operation named findStock(), which does the stock lookup. To send result back to
	// sender, we use the pipeTo() method
	println("\nStep 3: Create DonutStockActor")//------------------------------------------------

	class DonutStockActor extends Actor with ActorLogging {

		//Receive: reacts to checkstock message
		//Then it delegates the call to a Future operation named findStock
		// which does stock lookup.
		//To send result back to sender, use pipeTo()
		def receive ={

			case CheckStock(name) => {
				log.info(s"Checking the stock for $name donut")
				findStock(name).pipeTo(sender)
			}
		}

		def findStock(name: String): Future[Int] = Future {
			//assume a long running data base operation to
			// find stock for the given donut name
			100
		}

	}

	//actually creating the actor
	val donutInfoActor: ActorRef = system.actorOf(Props[DonutStockActor], name="DonutStockActor")


	// Ask pattern will return a future. We use for loop to print response from DonutInfoActor
	println("\nStep 4: Akka Ask Pattern and future pipeTo()")//------------------------------------------------
	implicit val timeout = Timeout(5 second)

	val vanillaDonutFound: Future[Int] = (donutInfoActor ? CheckStock("vanilla")).mapTo[Int]
	for {
		found <- vanillaDonutFound
	} yield (println(s"Vanilla donut found = $found"))

	val glazedDonutFound = donutInfoActor ? Info("glazed")
	for {
		found <- glazedDonutFound
	} yield (println(s"Glazed donut found = $found"))


	//close the system
	println("\nStep 5: Close the actor system")//------------------------------------------------

	val isTerminated = system.terminate()
	Thread.sleep(5000)


}


/**
  * Actor Hierarchy:
  * -- (1) root guardian: topmost actor which monitors the entire actor system
  * -- (2) system guardian: under root guardian: the top-level actor in charge of any
  * system-level actors
  * -- (3) user guardian: the top-level actor hierarchy for actors that we create in the actor
  * system.
  *
  * Example akka://DonutStoreActorSystem/user/DonutInfoActor
  * so DonutInfoActor is a user guardian actor
  * Path to an actor is similar to file path
  *
  * Lookup: can use actor's path to find it in an actor system.
  */

object ActorLookup extends App {

	// Goal: Lookup: can use actor's path to find it in an actor system.
	//Using the tell pattern here

	//Create actor system
	println("\nStep 1: create actor system")//------------------------------------------------
	val system = ActorSystem("DonutStoreActorSystem")

	//create message passing protocol: this class represents the message sent
	println("\nStep 2: define the message passing protocol for the actor")//------------------------------------------------

	object DonutStoreProtocol {
		case class Info(name: String)
	}
	import DonutStoreProtocol._

	//create actor: make a class extending the Actor trait
	//DonutStockActor REACTS to a CheckStock message and delegates the call to a future
	// operation named findStock(), which does the stock lookup. To send result back to
	// sender, we use the pipeTo() method
	println("\nStep 3: Create DonutStockActor")//------------------------------------------------

	class DonutInfoActor extends Actor with ActorLogging {

		def receive ={

			//just receiving
			case Info(name) => log.info(s"Found $name donut")
		}

	}

	//actually creating the actor
	val donutInfoActor: ActorRef = system.actorOf(Props[DonutInfoActor], name="DonutInfoActor")


	// Tell pattern
	println("\nStep 4: Akka Tell Pattern")//------------------------------------------------
	donutInfoActor ! Info("vanilla") //telling, sending the vanilla message


	//find the actor using its path
	println("\nStep 5: Find an actor using actorSelection() method")//------------------------------------------------
	system.actorSelection("/user/DonutInfoActor") ! Info("chocolate")

	// can also use actorSelection() to send message to ALL THE ACTORS IN THE SYSTEM
	// by using a wildcard: sending message to all user actors
	system.actorSelection("/user/*") ! Info("vanilla and chocolate")


	//close the system
	println("\nStep 6: Close the actor system")//------------------------------------------------

	val isTerminated = system.terminate()
	Thread.sleep(5000)
}




object ChildActors extends App {

	// Goal: child actors can be spawned from within an actor

	//Create actor system
	println("\nStep 1: create actor system")//------------------------------------------------
	val system = ActorSystem("DonutStoreActorSystem")

	//create message passing protocol: this class represents the message sent
	println("\nStep 2: define the message passing protocol for the actor")//------------------------------------------------

	object DonutStoreProtocol {
		case class Info(name: String)
	}
	import DonutStoreProtocol._

	//Create a BakingActor, child of DonutInfoActor below
	println("\nStep 3: Create DonutInfoActor and child BakingActor")//------------------------------------------------

	class BakingActor extends Actor with ActorLogging {
		def receive = {
			case Info(name) => log.info(s"BakingActor baking $name donut")
		}
	}


	class DonutInfoActor extends Actor with ActorLogging {

		val bakingActor = context.actorOf(Props[BakingActor], name="BakingActor")


		//DonutInfoActor is under /user hierarchy: /user/DonutInfoActor
		//That means the child BakingActor hierarchy an dpath are: /user/DonutInfoActor/BakingActor
		//This makes receive easy: just forward the msg to baking actor
		def receive ={

			case msg@Info(name) => {
				log.info(s"DonutInfoActor receiving: Found $name donut")
				bakingActor forward msg
			}
		}

	}

	//Create donutinfoactor since it is responsible for managing creation of child actor bakingactor
	println("\nStep 4: Create DonutInfoActor")//------------------------------------------------
	val donutInfoActor: ActorRef = system.actorOf(Props[DonutInfoActor], name="DonutInfoActor")


	// Send Info message to the DonutInfoActor, which will internally forward the
	//message to child BakingActor
	println("\nStep 5: Akka Tell Pattern")//------------------------------------------------
	donutInfoActor ! Info("vanilla") //telling, sending the vanilla message



	//close the system
	println("\nStep 6: Close the child actor system")//------------------------------------------------

	val isTerminated = system.terminate()
	Thread.sleep(5000)
}



/**
  * Every akka actor follows a lifecycle, representing the main events of an actor from
  * creation to deletion.
  */
object ActorLifeCycle extends App {
	//Create actor system
	println("\nStep 1: create actor system")//------------------------------------------------
	val system = ActorSystem("DonutStoreActorSystem")

	//create message passing protocol: this class represents the message sent
	println("\nStep 2: define the message passing protocol for the actor")//------------------------------------------------

	object DonutStoreProtocol {
		case class Info(name: String)
	}
	import DonutStoreProtocol._

	//Create a BakingActor, child of DonutInfoActor below
	println("\nStep 3: Create DonutInfoActor and child BakingActor")//------------------------------------------------

	//Overriding the actor lifecycle events and just log a message to know when they happen
	class BakingActor extends Actor with ActorLogging {

		//making the lifecycle methods explicit (they are called anyway underneath)
		override def preStart(): Unit = log.info("prestart")
		override def postStop(): Unit = log.info("postStop")
		override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info("preRestart")
		override def postRestart(reason: Throwable): Unit = log.info("postRestart")

		def receive = {
			case Info(name) => log.info(s"BakingActor baking $name donut")
		}
	}


	class DonutInfoActor extends Actor with ActorLogging {

		override def preStart(): Unit = log.info("prestart")
		override def postStop(): Unit = log.info("postStop")
		override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info("preRestart")
		override def postRestart(reason: Throwable): Unit = log.info("postRestart")


		val bakingActor = context.actorOf(Props[BakingActor], name = "BakingActor")

		def receive = {
			case msg @ Info(name) =>
				log.info(s"Found $name donut")
				bakingActor forward msg
		}
	}


	//Create donutinfoactor since it is responsible for managing creation of child actor bakingactor
	println("\nStep 4: Create DonutInfoActor")//------------------------------------------------
	val donutInfoActor: ActorRef = system.actorOf(Props[DonutInfoActor], name="DonutInfoActor")


	// Send Info message to the DonutInfoActor, which will internally forward the
	//message to child BakingActor
	println("\nStep 5: Akka Tell Pattern")//------------------------------------------------
	donutInfoActor ! Info("vanilla") //telling, sending the vanilla message
	Thread.sleep(5000)


	//close the system
	println("\nStep 6: Close the child actor system")//------------------------------------------------

	val isTerminated = system.terminate()
	isTerminated.onComplete {
		case Success(result) => println("Successfully terminated actor system")
		case Failure(e) => println("Failed to terminate")
	}
	Thread.sleep(5000)
}

/**
Step 4: Create DonutInfoActor

Step 5: Akka Tell Pattern
[INFO] [05/15/2019 04:12:37.747] [DonutStoreActorSystem-akka.actor.default-dispatcher-2] [akka://DonutStoreActorSystem/user/DonutInfoActor] prestart
[INFO] [05/15/2019 04:12:37.747] [DonutStoreActorSystem-akka.actor.default-dispatcher-5] [akka://DonutStoreActorSystem/user/DonutInfoActor/BakingActor] prestart
[INFO] [05/15/2019 04:12:37.754] [DonutStoreActorSystem-akka.actor.default-dispatcher-2] [akka://DonutStoreActorSystem/user/DonutInfoActor] Found vanilla donut
[INFO] [05/15/2019 04:12:37.755] [DonutStoreActorSystem-akka.actor.default-dispatcher-5] [akka://DonutStoreActorSystem/user/DonutInfoActor/BakingActor] BakingActor baking vanilla donut

Step 6: Close the child actor system
[INFO] [05/15/2019 04:12:42.776] [DonutStoreActorSystem-akka.actor.default-dispatcher-2] [akka://DonutStoreActorSystem/user/DonutInfoActor/BakingActor] postStop
[INFO] [05/15/2019 04:12:42.784] [DonutStoreActorSystem-akka.actor.default-dispatcher-5] [akka://DonutStoreActorSystem/user/DonutInfoActor] postStop
Successfully terminated actor system
  */









/**
  * can use akka.actor.PoisonPill, a special message to terminate an actor
  * When this is sent, we can see the actor "stop" event being triggered in the lifecycle methods
  */
object ActorPoisonPill extends App {
	//Create actor system
	println("\nStep 1: Create actor system")//------------------------------------------------
	val system = ActorSystem("DonutStoreActorSystem")

	//create message passing protocol: this class represents the message sent
	println("\nStep 2: Define the message passing protocol for the actor")//------------------------------------------------

	object DonutStoreProtocol {
		case class Info(name: String)
	}
	import DonutStoreProtocol._

	//Create a BakingActor, child of DonutInfoActor below
	println("\nStep 3: Create DonutInfoActor and child BakingActor")//------------------------------------------------

	//Overriding the actor lifecycle events and just log a message to know when they happen
	class BakingActor extends Actor with ActorLogging {

		//making the lifecycle methods explicit (they are called anyway underneath)
		override def preStart(): Unit = log.info("prestart")
		override def postStop(): Unit = log.info("postStop")
		override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info("preRestart")
		override def postRestart(reason: Throwable): Unit = log.info("postRestart")

		def receive = {
			case Info(name) => log.info(s"BakingActor baking $name donut")
		}
	}


	class DonutInfoActor extends Actor with ActorLogging {

		override def preStart(): Unit = log.info("prestart")
		override def postStop(): Unit = log.info("postStop")
		override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info("preRestart")
		override def postRestart(reason: Throwable): Unit = log.info("postRestart")


		val bakingActor = context.actorOf(Props[BakingActor], name = "BakingActor")

		def receive = {
			case msg @ Info(name) =>
				log.info(s"Found $name donut")
				bakingActor forward msg
		}
	}


	//Create donutinfoactor since it is responsible for managing creation of child actor bakingactor
	println("\nStep 4: Create DonutInfoActor")//---------------------------------------------------------------
	val donutInfoActor: ActorRef = system.actorOf(Props[DonutInfoActor], name="DonutInfoActor")


	// Use Tell pattern to send the poisonpill message to DonutInfoActor
	//See that sending another message after the PoisonPill doesn't work (the actor is  stopped)
	println("\nStep 5: Akka Tell Pattern")//------------------------------------------------

	donutInfoActor ! Info("vanilla") //telling, sending the vanilla message
	donutInfoActor ! Info("strawberry")
	donutInfoActor ! Info("cherry")
	donutInfoActor ! Info("chocolate")
	donutInfoActor ! PoisonPill
	donutInfoActor ! Info("plain")
	donutInfoActor ! Info("glazed")
	Thread.sleep(5000)


	//close the system
	println("\nStep 6: Close the child actor system")//------------------------------------------------

	val isTerminated = system.terminate()
	isTerminated.onComplete {
		case Success(result) => println("Successfully terminated actor system")
		case Failure(e) => println("Failed to terminate")
	}
	Thread.sleep(5000)
}

/**
Step 5: Akka Tell Pattern
[INFO] [05/15/2019 04:10:04.443] [DonutStoreActorSystem-akka.actor.default-dispatcher-3] [akka://DonutStoreActorSystem/user/DonutInfoActor/BakingActor] prestart
[INFO] [05/15/2019 04:10:04.444] [DonutStoreActorSystem-akka.actor.default-dispatcher-5] [akka://DonutStoreActorSystem/user/DonutInfoActor] prestart
[INFO] [05/15/2019 04:10:04.444] [DonutStoreActorSystem-akka.actor.default-dispatcher-5] [akka://DonutStoreActorSystem/user/DonutInfoActor] Found vanilla donut
[INFO] [05/15/2019 04:10:04.445] [DonutStoreActorSystem-akka.actor.default-dispatcher-4] [akka://DonutStoreActorSystem/user/DonutInfoActor/BakingActor] BakingActor baking vanilla donut
[INFO] [05/15/2019 04:10:04.445] [DonutStoreActorSystem-akka.actor.default-dispatcher-5] [akka://DonutStoreActorSystem/user/DonutInfoActor] Found strawberry donut
[INFO] [05/15/2019 04:10:04.445] [DonutStoreActorSystem-akka.actor.default-dispatcher-4] [akka://DonutStoreActorSystem/user/DonutInfoActor/BakingActor] BakingActor baking strawberry donut
[INFO] [05/15/2019 04:10:04.445] [DonutStoreActorSystem-akka.actor.default-dispatcher-5] [akka://DonutStoreActorSystem/user/DonutInfoActor] Found cherry donut
[INFO] [05/15/2019 04:10:04.445] [DonutStoreActorSystem-akka.actor.default-dispatcher-4] [akka://DonutStoreActorSystem/user/DonutInfoActor/BakingActor] BakingActor baking cherry donut
[INFO] [05/15/2019 04:10:04.446] [DonutStoreActorSystem-akka.actor.default-dispatcher-5] [akka://DonutStoreActorSystem/user/DonutInfoActor] Found chocolate donut
[INFO] [05/15/2019 04:10:04.446] [DonutStoreActorSystem-akka.actor.default-dispatcher-4] [akka://DonutStoreActorSystem/user/DonutInfoActor/BakingActor] BakingActor baking chocolate donut
[INFO] [05/15/2019 04:10:04.461] [DonutStoreActorSystem-akka.actor.default-dispatcher-2] [akka://DonutStoreActorSystem/user/DonutInfoActor/BakingActor] postStop
[INFO] [05/15/2019 04:10:04.466] [DonutStoreActorSystem-akka.actor.default-dispatcher-5] [akka://DonutStoreActorSystem/user/DonutInfoActor] postStop
[INFO] [05/15/2019 04:10:04.470] [DonutStoreActorSystem-akka.actor.default-dispatcher-2] [akka://DonutStoreActorSystem/user/DonutInfoActor] Message [Actors.ActorPoisonPill$DonutStoreProtocol$Info] without sender to Actor[akka://DonutStoreActorSystem/user/DonutInfoActor#-905492795] was not delivered. [1] dead letters encountered. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
[INFO] [05/15/2019 04:10:04.471] [DonutStoreActorSystem-akka.actor.default-dispatcher-2] [akka://DonutStoreActorSystem/user/DonutInfoActor] Message [Actors.ActorPoisonPill$DonutStoreProtocol$Info] without sender to Actor[akka://DonutStoreActorSystem/user/DonutInfoActor#-905492795] was not delivered. [2] dead letters encountered. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.

  */










/**
  * Error kernel pattern: mandates that failures are isolated and localized, as opposed to
  * crashing an entire system.
  * Actors form a hierarchy: we use that features to make Actors supervise and react to failures of
  * their child actors.
  *
  * DonutStockActor forwards work to a child actor named DonutStockWorkeractor
  * Also, since this is a child, it will be supervised by its parent.
  *
  */
object ErrorKernelSupervision extends App {

	import akka.pattern.ask
	import akka.actor.SupervisorStrategy.{Restart, Escalate}


	println("Step 1: Create an actor system")//------------------------------------------------
	val system = ActorSystem("DonutStoreActorSystem")



	println("\nStep 2: Define the message passing protocol for the DonutStoreActor")//------------------------------------------------

	object DonutStoreProtocol {
		case class Info(name: String)
		case class CheckStock(name: String)
		case class WorkerFailedException(error: String) extends Exception(error)
	}
	import DonutStoreProtocol._


	//DonutStockActor gets CheckStock messages, and will forward the request from
	//CheckStock to a child actor for processing.
	//The child actor is of type DonutStockWorkerActor
	//DonutStockActor will supervise the child actor (worker) by providing a
	//SupervisorStrategy
	//Actor will react to exceptions and attempt to restart the child actor.
	//For all other exceptions, we assume the DonutStockActor is unable to
	//handle those and will escalate the exceptions up the actor hierarchy.
	println("\nStep 3: Create DonutStockActor")//------------------------------------------------

	class DonutStockActor extends Actor with ActorLogging {

		override def supervisorStrategy: SupervisorStrategy =
			OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1 seconds) {
				case _ : WorkerFailedException => {
					log.error("Worker failed exception, will restart.")
					Restart
				}

					case _: Exception => {
						log.error("Worker failed for some other reason, escalate up the hierarchy")
						Escalate
					}
			}



		//child actor
		val workerActor = context.actorOf(Props[DonutStockWorkerActor], name="DonutStockWorkerActor")


		def receive = {
			case checkStock @ CheckStock(name) => {
				log.info(s"Checking stock for $name donut")
				workerActor forward checkStock  //the parent actor (this class) forwards the message to child
				// actor, workerActor
			}
		}
	}



	println("\nStep 4: Worker Actor called DonutStockWorker")//------------------------------------------------

	class DonutStockWorkerActor extends Actor with ActorLogging {

		//Tapping into postRestart lifecycle method to know when this child actor
		//gets restarted
		//When the child receives messages from CheckStock, it will delegate work to
		//findstock() method, and then will stop processing any other messages
		//because we call context.stop(self)

		@throws[Exception](classOf[Exception])
		override def postRestart(reason: Throwable): Unit = {
			log.info(s"restarting ${self.path.name} because of $reason")
		}

		def receive = {
			case CheckStock(name) => {
				findStock(name) //error thrown
				context.stop(self) //after processing checkstock messages, stop processing another messages.
			}
		}

		def findStock(name: String): Int = {
			log.info(s"Finding stock for donut = $name")
			100
			//throw new IllegalStateException("boom") // Will Escalate the exception up the hierarchy
			throw new WorkerFailedException("boom") // Will Restart DonutStockWorkerActor
		}

	}




	println("\nStep 5: Define DonutStockactor")//------------------------------------------------
	val donutStockActor = system.actorOf(Props[DonutStockActor], name = "DonutStockActor")



	println("\nStep 6: Send CheckStock message using Ask Pattern")//------------------------------------------------
	implicit val timeout = Timeout(5 second)

	val vanillaDonutStock: Future[Int] = (donutStockActor ? CheckStock("vanilla")).mapTo[Int]
	for {
		amount <- vanillaDonutStock
	} yield println(s"Vanilla donut stock = $amount")


	Thread.sleep(5000)


	println("\nStep 7: Close the actor system")//------------------------------------------------
	system.terminate()
	Thread.sleep(5000)
}

/**
 //WHEN UNCOMMENTING ILLEGAL EXCEPTION:
  Step 5: Define DonutStockactor

Step 6: Send CheckStock message using Ask Pattern
[INFO] [05/15/2019 04:16:30.961] [DonutStoreActorSystem-akka.actor.default-dispatcher-4] [akka://DonutStoreActorSystem/user/DonutStockActor] Checking stock for vanilla donut
[INFO] [05/15/2019 04:16:30.962] [DonutStoreActorSystem-akka.actor.default-dispatcher-2] [akka://DonutStoreActorSystem/user/DonutStockActor/DonutStockWorkerActor] Finding stock for donut = vanilla
[ERROR] [05/15/2019 04:16:30.982] [DonutStoreActorSystem-akka.actor.default-dispatcher-2] [akka://DonutStoreActorSystem/user/DonutStockActor] Worker failed for some other reason, escalate up the hierarchy
[ERROR] [05/15/2019 04:16:30.992] [DonutStoreActorSystem-akka.actor.default-dispatcher-2] [akka://DonutStoreActorSystem/user/DonutStockActor] boom
java.lang.IllegalStateException: boom
	at Actors.ErrorKernelSupervision$DonutStockWorkerActor.findStock(ActorExamples.scala:769)
	at Actors.ErrorKernelSupervision$DonutStockWorkerActor$$anonfun$receive$13.applyOrElse(ActorExamples.scala:761)
	at akka.actor.Actor.aroundReceive(Actor.scala:517)
	at akka.actor.Actor.aroundReceive$(Actor.scala:515)
	at Actors.ErrorKernelSupervision$DonutStockWorkerActor.aroundReceive(ActorExamples.scala:746)
	at akka.actor.ActorCell.receiveMessage(ActorCell.scala:588)
	at akka.actor.ActorCell.invoke(ActorCell.scala:557)
	at akka.dispatch.Mailbox.processMailbox(Mailbox.scala:258)
	at akka.dispatch.Mailbox.run(Mailbox.scala:225)
	at akka.dispatch.Mailbox.exec(Mailbox.scala:235)
	at akka.dispatch.forkjoin.ForkJoinTask.doExec(ForkJoinTask.java:260)
	at akka.dispatch.forkjoin.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:1339)
	at akka.dispatch.forkjoin.ForkJoinPool.runWorker(ForkJoinPool.java:1979)
	at akka.dispatch.forkjoin.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)


Step 7: Close the actor system
  */


/** -----------------------------------------------------------
 WHEN UNCOMMENTING WORKER FAILED EXCEPTION:
  */