package njanma

import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import io.circe.parser._
import io.circe.syntax._
import njanma.Request.Ping
import njanma.Response.Pong

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration._

trait PingFlow {
  implicit def materializer: ActorMaterializer

  implicit lazy val timeout: Timeout = Timeout(5 seconds)
  //  def webSocketRoute: Route = path("ws-echo")(handleWebSocketMessages(flow))

  def flow: Flow[Message, Message, NotUsed] = {
    val processorsCount = Runtime.getRuntime.availableProcessors()
    val value = Flow[Message]
      .collect {
        case tm: TextMessage => tm.textStream
      }
      .mapAsync(processorsCount)(in =>
        for {
          text <- in.runFold("")(_ + _)
          json <- Future.fromTry(parse(text).toTry)
          res <- Future.fromTry(json.as[Request].toTry)
        } yield res)
      .collect[Pong] {
        case Ping(num) => Pong(num + 1)
      }
      .map(out => TextMessage(out.toString))

    ???
  }

}
