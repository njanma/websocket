package njanma

import akka.NotUsed
import akka.actor.{ActorRef, ActorSelection}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import io.circe.parser._
import io.circe.syntax._
import njanma.dto.Request.{AddTable, Ping, UpdateTable}
import njanma.dto.Response.Pong
import njanma.dto.{Request, Response}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait WebSocketFlow {

  implicit lazy val timeout: Timeout = Timeout(10 second)

  implicit def materializer: ActorMaterializer

  def tableActor: ActorSelection

  def webSocketRoute: Route = path("")(handleWebSocketMessages(flow))

  private def flow: Flow[Message, Message, NotUsed] = {
    Flow[Message]
      .collect {
        case tm: TextMessage => tm.textStream
      }
      .mapAsync(Runtime.getRuntime.availableProcessors)(
        in =>
          for {
            text <- in.runFold("")(_ + _)
            json <- Future.fromTry(parse(text).toTry)
            res <- Future.fromTry(json.as[Request].toTry)
          } yield res
      )
      .mapAsync(Runtime.getRuntime.availableProcessors) {
        case Ping(num) => Future(Pong(num))
        case addTable: AddTable => (tableActor ? addTable).mapTo[Response]
        case updateTable: UpdateTable => (tableActor ? updateTable).mapTo[Response]
      }
      .mapAsync(Runtime.getRuntime.availableProcessors)(
        out => Future(TextMessage(out.asJson.spaces2))
      )
  }
}
