package njanma.dto

import io.circe.Encoder
import njanma.dto.Request.TableRequest

sealed abstract class Response(val $type: String)

object Response {

  case class LoginFailed() extends Response("login_failed")

  case class LoginSuccessful(user_type: String)
      extends Response("login_successful")

  case class Pong(seq: Int) extends Response("pong")

  case class TableAdded(after_id: Option[Int], table: TableRequest)
      extends Response("table_added")

  case class TableRemoved(id: Int) extends Response("table_removed")

  case class TableUpdated(table: TableRequest) extends Response("table_updated")

  implicit val pongEncoder: Encoder[Pong] =
    Encoder.forProduct2("$type", "seq")(pong => (pong.$type, pong.seq))

  implicit val loginSuccessfulEncoder: Encoder[LoginSuccessful] =
    Encoder.forProduct2("$type", "user_type")(
      loginSuccessful => (loginSuccessful.$type, loginSuccessful.user_type)
    )

  implicit val loginFailedEncoder: Encoder[LoginFailed] =
    Encoder.forProduct1("$type")(loginFailed => loginFailed.$type)

  implicit val responseEncoder: Encoder[Response] = Encoder.instance {
    case pong @ Pong(_) => pongEncoder(pong)
    case loginSuccessful @ LoginSuccessful(_) =>
      loginSuccessfulEncoder(loginSuccessful)
    case loginFailed @ LoginFailed() => loginFailedEncoder(loginFailed)
  }
}
