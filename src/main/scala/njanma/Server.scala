package njanma

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ActorRef, ActorSelection, ActorSystem, OneForOneStrategy}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.stream.ActorMaterializer
import njanma.actor.TableActor
import njanma.config.DbConfig
import njanma.repository.{DbConnector, TableRepository}
import pureconfig.loadConfigOrThrow
import pureconfig.generic.auto._

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Server extends App with WebSocketFlow {

  implicit val system: ActorSystem = ActorSystem("server-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val dbConnector: DbConnector = DbConnector(loadConfigOrThrow[DbConfig].db)

  val tableRepository: TableRepository = new TableRepository(dbConnector)
  val tableSupervisor: ActorRef = system.actorOf(
    BackoffSupervisor.props(
      Backoff
        .onStop(
          TableActor.props(tableRepository),
          "table-actor",
          3 seconds,
          30 seconds,
          0
        )
        .withSupervisorStrategy(OneForOneStrategy(5, 5 second) {
          case _ => Restart
        })
    ),
    "table-actor"
  )
  val tableActor: ActorSelection = system.actorSelection("/user/table-actor")

  lazy val routes: Route = webSocketRoute

  val binding: Future[Http.ServerBinding] =
    Http().bindAndHandle(routes, "localhost", 9000)

  binding.onComplete {
    case Success(bound) =>
      println(
        s"Server started on ws://${bound.localAddress.getHostString}:${bound.localAddress.getPort}"
      )
    case Failure(exception) =>
      Console.err.println(s"Server couldn't start!")
      exception.printStackTrace()
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
