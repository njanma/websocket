package njanma

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import njanma.actor.TableActor
import njanma.repository.{DbConnector, TableRepository}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object Server extends App with WebSocketFlow {

  implicit val system: ActorSystem = ActorSystem("server-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  lazy val dbConnector: DbConnector = DbConnector()

  val tableRepository: TableRepository = new TableRepository(dbConnector)
  val tableActor: ActorRef =
    system.actorOf(TableActor.props(tableRepository), "table-actor")

  lazy val routes: Route = webSocketRoute

  val binding: Future[Http.ServerBinding] =
    Http().bindAndHandle(routes, "localhost", 9000)

  binding.onComplete {
    case Success(bound) =>
      println(
        s"Server started on ws://${bound.localAddress.getHostString}:${bound.localAddress.getPort}"
      )
    case Failure(exception) =>
      Console.err.println(s"Server could not start!")
      exception.printStackTrace()
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
