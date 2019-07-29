package njanma

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
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
import scala.util.Try

trait WebSocketFlow {

  implicit lazy val timeout: Timeout = Timeout(10 second)

  implicit def materializer: ActorMaterializer

  def tableActor: ActorRef

  def webSocketRoute: Route = path("")(authenticateBasic(realm = "sec", userPassAuthenticator) { usr =>
    authorizeAsync(_ => hasPermissions(usr)) {
      handleWebSocketMessages(flow)
    }
  })

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

  def userPassAuthenticator(credentials: Credentials): Option[User] = {
    credentials match {
      case p@Credentials.Provided(id) =>
        if (p.verify("p4ssw0rd")) Some(User(id))
        else None
      case _ => None
    }
  }

  case class User(name: String)

  val validUsers = Set("john", "peter", "tiger", "susan")

  def hasPermissions(user: User): Future[Boolean] = {
    Future.successful(validUsers.contains(user.name))
  }

  final class ApiTokenHeader(token: String) extends ModeledCustomHeader[ApiTokenHeader] {
    override def renderInRequests = true

    override def renderInResponses = false

    override val companion = ApiTokenHeader

    override def value: String = token
  }

  object ApiTokenHeader extends ModeledCustomHeaderCompanion[ApiTokenHeader] {
    override val name = "apiKey"

    override def parse(value: String) = Try(new ApiTokenHeader(value))
  }

}
