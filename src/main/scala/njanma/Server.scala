package njanma

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import njanma.db.{DbConnector, UserRepositoryActor}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object Server extends App with WebsocketFlow {

  implicit val system: ActorSystem = ActorSystem("server-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  lazy val dbConnector: DbConnector = DbConnector()

  val userRepository: ActorRef = system.actorOf(UserRepositoryActor.props(dbConnector))
  val userActor: ActorRef = system.actorOf(UserActor.props(userRepository), "userActor")

  lazy val routes: Route = webSocketRoute

  val binding: Future[Http.ServerBinding] = Http().bindAndHandle(routes, "localhost", 9000)

  binding.onComplete {
    case Success(bound) =>
      println(s"Server started on ws://${bound.localAddress.getHostString}:${bound.localAddress.getPort}")
    case Failure(exception) =>
      Console.err.println(s"Server could not start!")
      exception.printStackTrace()
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
