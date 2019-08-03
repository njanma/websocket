package njanma

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import njanma.config.AppConfig
import njanma.dto.Request.Ping
import pureconfig.loadConfigOrThrow
import pureconfig.generic.auto._
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Promise}

object TestApp extends App {

  implicit val system: ActorSystem = ActorSystem("test-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system)
    .withSupervisionStrategy(_ => Supervision.Restart))
  implicit val executionContext: ExecutionContext = system.dispatcher
  private val settings: ClientConnectionSettings = ClientConnectionSettings(system)
  private val serverConfig = loadConfigOrThrow[AppConfig].server

  private val flow: Flow[Message, Message, Promise[Option[Message]]] = Flow.fromSinkAndSourceMat(
    Sink.foreach[Message](println),
    Source.single(TextMessage(Ping(1).asJson.spaces2))
      .concatMat(Source.maybe[Message])(Keep.right))(Keep.right)

  private val credentials: scala.collection.immutable.Seq[HttpHeader] = Authorization(BasicHttpCredentials("user", "password"))::Nil
  Http().singleWebSocketRequest(
    WebSocketRequest(s"ws://${serverConfig.host}:${serverConfig.port}/${serverConfig.connectionPath}",  extraHeaders = credentials, None),
    flow, Http().defaultClientHttpsContext, None,
    settings)
}
