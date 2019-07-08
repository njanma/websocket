package njanma

import io.circe.Decoder
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.syntax._
import cats.syntax.functor._
sealed abstract class Request($type: String)

object Request {

   case class Login(username: String, password: String) extends Request("login")

   case class Ping(seq: Int) extends Request("ping")

   case object SubscribeTables extends Request("subscribe_tables")

   case object UnsubscribeTables extends Request("unsubscribe_tables")

   case class AddTable(after_id: Option[Int], table: Table)
    extends Request("add_table")

   case class RemoveTable(id: Int) extends Request("remove_table")

   case class UpdateTable(table: Table) extends Request("update_table")

  implicit val decodeEvent: Decoder[Request] =
    List[Decoder[Request]](
      Decoder[Login].widen,
      Decoder[Ping].widen,
      Decoder[AddTable].widen,
      Decoder[RemoveTable].widen,
      Decoder[UpdateTable].widen
    ).reduceLeft(_ or _)
  //  implicit val requestDecoder: Decoder[Request] = deriveDecoder[Request]
  //  implicit val requestEncoder: Encoder[Request] = deriveEncoder[Request]
}
