package AkkaActors

import akka.actor._
import akka.util.Timeout

import scala.collection.immutable

//futures
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try //places default thread pool in scope so Future can be
// executed asynchronously.
import scala.concurrent.{Await, Future, Promise, future}
import scala.concurrent.duration._
import scala.util.{Success, Failure}


/**
  * http://allaboutscala.com/scala-frameworks/akka/#what-is-akka
  */

//Routing: allows us to send messages to predefined set of routees (receivers)
//Instead of having DonutStockActor send messages to DonutStockWorkerActor (child),
//it will send messages to a pool of DonutStockWorkerActor.
//RoundRobinPool logic is a routing strategy of sending messages in RoundRobin way amount the routees.
object AkkaRoundRobinPool extends App {

	import akka.pattern.ask
	import akka.actor.SupervisorStrategy.{Restart, Escalate}
	import akka.routing.DefaultResizer
	import akka.routing.RoundRobinPool



	println("Step 1: Create an actor system")//------------------------------------------------
	val system = ActorSystem("DonutStoreActorSystem")



	//Define message passing protocols to our actors
	println("\nStep 2: Define the message passing protocol for our DonutStoreActor")//------------------------------------------------

	object DonutStoreProtocol {
		case class Info(name: String)
		case class CheckStock(name: String)
		case class WorkerFailedException(error: String) extends Exception(error)
	}
	import DonutStoreProtocol._



	println("\nStep 3: Create the DonutStockActor") //------------------------------------------------------------
	//this actor will forward messages to worker child (not to a single one but to a pool)


	//DonutStockActor gets CheckStock messages, and will forward the request from
	//CheckStock to a child actor for processing.
	//The child actor is of type DonutStockWorkerActor
	//DonutStockActor will supervise the child actor (worker) by providing a
	//SupervisorStrategy
	//Actor will react to exceptions and attempt to restart the child actor.
	//For all other exceptions, we assume the DonutStockActor is unable to
	//handle those and will escalate the exceptions up the actor hierarchy.
	class DonutStockActor extends Actor with ActorLogging {

		override def supervisorStrategy: SupervisorStrategy =
			OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 5 seconds) {
				case _: WorkerFailedException =>
					log.error("Worker failed exception, will restart.")
					Restart

				case _: Exception =>
					log.error("Worker failed, will need to escalate up the hierarchy")
					Escalate
			}

		// We will not create one worker actor.
		// val workerActor = context.actorOf(Props[DonutStockWorkerActor], name = "DonutStockWorkerActor")

		// We are using a resizable RoundRobinPool.
		val resizer = DefaultResizer(lowerBound = 5, upperBound = 10)
		val props = RoundRobinPool(5, Some(resizer), supervisorStrategy = supervisorStrategy)
			.props(Props[DonutStockWorkerActor])
		val donutStockWorkerRouterPool: ActorRef = context.actorOf(props, "DonutStockWorkerRouter")

		def receive = {
			case checkStock @ CheckStock(name) =>
				log.info(s"Checking stock for $name donut")
				donutStockWorkerRouterPool forward checkStock
		}
	}



	println("\nStep 4: Worker Actor called DonutStockWorkerActor")

	class DonutStockWorkerActor extends Actor with ActorLogging {

		//Tapping into postRestart lifecycle method to know when this child actor gets restarted
		//When the child receives messages from CheckStock, it will delegate work to
		//findstock() method, and then will stop processing any other messages
		//because we call context.stop(self)

		@throws[Exception](classOf[Exception])
		override def postRestart(reason: Throwable): Unit = {
			log.info(s"restarting ${self.path.name} because of $reason")
		}

		def receive = {
			case CheckStock(name) => {
				sender ! findStock(name)
			}
		}

		def findStock(name: String): Int = {
			log.info(s"Finding stock for donut = $name")
			100
			//throw new IllegalStateException("boom") // Will Escalate the exception up the hierarchy
			//throw new WorkerFailedException("boom") // Will Restart DonutStockWorkerActor
		}

	}





	println("\nStep 5: Define Donutstock actor")
	val donutStockActor = system.actorOf(Props[DonutStockActor], name = "DonutStockActor")




	println("\nStep 6: Use Akka Ask Pattern and send a bunch of requests to DonutStockActor")
	implicit val timeout = Timeout(5 second)

	//val seq: Future[List[Any]] = Future.sequence(futureOperations)
	val vanillaStockRequests: Seq[Future[Int]] = (1 to 10).map(_ => (donutStockActor ? CheckStock("vanilla")).mapTo[Int])



	/*def async1(list: Seq[Int]) = Future {list.map(_ + 1)}
	def async2(list: Seq[Int]) = Future {throw new IllegalArgumentException("ERROR HERE")}

	//note: Future.sequence(list(future(int))) turns this into ==> Future(list(int))
	val aResult = for {
		results <- Future.sequence(vanillaStockRequests) //monad mapping into future to get results = list(int)
		again1 <- async1(results)
		again2 <- async2(again1)
		c <- async1(again2)
	} yield println(s"vanilla stock results = $c")


	Thread.sleep(5000)
	println(aResult)*/


	//note: Future.sequence(list(future(int))) turns this into ==> Future(list(int))
	for {
		results <- Future.sequence(vanillaStockRequests) //monad mapping into future to get results = list(int)
	} yield println(s"vanilla stock results = $results")


	Thread.sleep(5000)
}