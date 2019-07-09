package njanma.dto

import cats.syntax.functor._
import io.circe.Decoder
import io.circe.generic.auto._

sealed abstract class Request(val $type: String)

object Request {

  case class Login(username: String, password: String) extends Request("login")

  case class Ping(seq: Int) extends Request("ping")

  case class SubscribeTables() extends Request("subscribe_tables")

  case class UnsubscribeTables() extends Request("unsubscribe_tables")

  case class TableRequest(id: Option[Int], name: String, participants: Int)

  case class AddTable(after_id: Option[Int], table: TableRequest)
      extends Request("add_table")

  case class RemoveTable(id: Int) extends Request("remove_table")

  case class UpdateTable(table: TableRequest) extends Request("update_table")

  implicit val decodeEvent: Decoder[Request] =
    List[Decoder[Request]](
      Decoder[Login].widen,
      Decoder[SubscribeTables].widen,
      Decoder[UnsubscribeTables].widen,
      Decoder[Ping].widen,
      Decoder[AddTable].widen,
      Decoder[RemoveTable].widen,
      Decoder[UpdateTable].widen
    ).reduceLeft(_ or _)
}
