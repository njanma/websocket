package njanma

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import njanma.actor.TableActor
import njanma.config.AppConfig
import njanma.repository.{DbConnector, TableRepository, UserRepository}
import njanma.security.UserAuthenticator
import org.flywaydb.core.Flyway
import pureconfig.loadConfigOrThrow
import pureconfig.generic.auto._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Server extends App with WebSocketFlow {

  implicit val system: ActorSystem = ActorSystem("server-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withSupervisionStrategy(_ => Supervision.Restart)
  )
  implicit val executionContext: ExecutionContext = system.dispatcher

  private val appConfig = loadConfigOrThrow[AppConfig]
  private val dbConnector: DbConnector = DbConnector(appConfig.db)

  val tableRepository: TableRepository = new TableRepository(dbConnector)
  val userRepository: UserRepository = new UserRepository(dbConnector)
  override val connectionPath: String = appConfig.server.connectionPath

  override val tableActor: ActorRef =
    system.actorOf(TableActor.props(tableRepository), "table-actor")
  override val userAuthenticator: UserAuthenticator = new UserAuthenticator(
    userRepository
  )

  lazy val routes: Route = webSocketRoute

  Flyway
    .configure()
    .dataSource(
      appConfig.db.dataSource,
      appConfig.db.user,
      appConfig.db.password
    )
    .load()
    .migrate()

  val binding: Future[Http.ServerBinding] =
    Http().bindAndHandle(routes, appConfig.server.host, appConfig.server.port)

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
