package njanma

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisherMessage
import akka.stream.impl.PublisherSource
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import io.circe.parser._
import io.circe.syntax._
import njanma.dto.Request.{AddTable, Ping, RemoveTable, SubscribeChanged, SubscribeTables, UpdateTable}
import njanma.dto.Response.Pong
import njanma.dto.{Request, Response}
import njanma.security.UserAuthenticator
import org.reactivestreams.Publisher

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait WebSocketFlow {

  implicit lazy val timeout: Timeout = Timeout(10 second)

  implicit def materializer: ActorMaterializer

  def tableActor: ActorRef
  def userAuthenticator: UserAuthenticator
  val connectionPath: String

  def webSocketRoute: Route =
    path(connectionPath)(
      authenticateBasic(realm = "sec", userAuthenticator.check) { usr =>
        authorize(userAuthenticator.hasPermissions(usr)) {
      handleWebSocketMessages(flow)
        }
      }
    )


  private def flow: Flow[Message, Message, NotUsed] = {
    Flow[Message]
      .collect {
        case TextMessage.Strict(text) =>
          for {
            json <- parse(text).toTry
            res <- json.as[Request].toTry
          } yield res
      }
      .filter(_.isSuccess)
      .map(_.get)
      .mapAsync(Runtime.getRuntime.availableProcessors) {
        case Ping(num)          => Future(Pong(num))
        case addTable: AddTable => (tableActor ? addTable).mapTo[Response]
        case updateTable: UpdateTable =>
          (tableActor ? updateTable).mapTo[Response]
        case subscribeTables: SubscribeTables =>
          (tableActor ? subscribeTables).mapTo[Response]
        case removeTable: RemoveTable =>
          (tableActor ? removeTable).mapTo[Response]
        case SubscribeChanged(changed) => Future(changed)
      }
      .mapAsync(Runtime.getRuntime.availableProcessors)(
        out => Future(TextMessage(out.asJson.spaces2))
      )
  }
}
