package njanma

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.http.scaladsl.{Http, settings}
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}

import scala.concurrent.{ExecutionContext, Future, Promise}

object TestApp extends App {

  implicit val system: ActorSystem = ActorSystem("test-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system)
    .withSupervisionStrategy(_ => Supervision.Restart))
  implicit val executionContext: ExecutionContext = system.dispatcher
  private val settings: ClientConnectionSettings = ClientConnectionSettings(system)

  val incoming: Sink[Message, Future[Done]] =
    Sink.foreach[Message] {
      case message: TextMessage.Strict =>
        println(message.text)
    }
  val outgoing = Source.single(TextMessage("""{ "$type": "ping", "seq": 1} """))

  private val flow: Flow[Message, Message, Promise[Option[Message]]] = Flow.fromSinkAndSourceMat(
    Sink.foreach[Message](println),
    Source.single(TextMessage("""{ "$type": "ping", "seq": 1} """))
      .concatMat(Source.maybe[Message])(Keep.right))(Keep.right)


  private val credentials: scala.collection.immutable.Seq[HttpHeader] = Authorization(BasicHttpCredentials("user", "password"))::Nil
  Http().singleWebSocketRequest(
    WebSocketRequest("ws://127.0.0.1:9000/connect",  extraHeaders = credentials, None),
    flow, Http().defaultClientHttpsContext, None,
    settings)
}
