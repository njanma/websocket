package njanma

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.Timeout
import io.circe.parser._
import io.circe.syntax._
import njanma.actor.UserConnection
import njanma.dto.{Request, Response}
import njanma.security.UserAuthenticator

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait WebSocketFlow {

  implicit lazy val timeout: Timeout = Timeout(10 second)

  implicit def materializer: ActorMaterializer
  def system: ActorSystem
  def tableActor: ActorRef
  def subscribingActor: ActorRef
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

  def flow: Flow[Message, Message, _] = {
    val connectedWsActor =
      system.actorOf(UserConnection.props(tableActor, subscribingActor))

    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message]
        .map {
          case TextMessage.Strict(text) =>
            for {
              json <- parse(text).toTry
              res <- json.as[Request].toTry
            } yield res
        }
        .filter(_.isSuccess)
        .map(_.get)
        .to(Sink.actorRef(connectedWsActor, PoisonPill))

    val outgoingMessages: Source[Message, NotUsed] =
      Source
        .actorRef[Response](10, OverflowStrategy.fail)
        .mapMaterializedValue { outgoingActor =>
          connectedWsActor ! UserConnection.Connected(outgoingActor)
          NotUsed
        }
        .mapAsync(Runtime.getRuntime.availableProcessors)(
          out => Future(TextMessage(out.asJson.spaces2))
        )
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
