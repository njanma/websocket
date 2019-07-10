package njanma.dto

import io.circe.Encoder
import njanma.dto.Request.TableRequest
import njanma.entity.Table

sealed abstract class Response(val $type: String)

object Response {

  case class LoginFailed() extends Response("login_failed")

  case class LoginSuccessful(user_type: String)
      extends Response("login_successful")

  case class Pong(seq: Int) extends Response("pong")

  case class TableAdded(after_id: Option[Int], table: TableRequest)
      extends Response("table_added")

  object TableAdded {
    def apply(table: Table): TableAdded =
      TableAdded(None, TableRequest(table.id, table.name, table.participants))
  }

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

  implicit val tableAddedEncoder: Encoder[TableAdded] =
    Encoder.forProduct3("$type", "after_id", "table")(
      tableAdded => (tableAdded.$type, tableAdded.after_id, tableAdded.table)
    )

  implicit val responseEncoder: Encoder[Response] = Encoder.instance {
    case pong @ Pong(_) => pongEncoder(pong)
    case loginSuccessful @ LoginSuccessful(_) =>
      loginSuccessfulEncoder(loginSuccessful)
    case loginFailed @ LoginFailed() => loginFailedEncoder(loginFailed)
    case tableAdded: TableAdded      => tableAddedEncoder(tableAdded)
  }
}
