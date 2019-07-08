package njanma

import io.circe.generic.JsonCodec
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

sealed abstract class Response($type: String)

object Response {

   case object LoginFailed extends Response("login_failed")

   case class LoginSuccessful(user_type: String)
    extends Response("login_successful")

   case class Pong(seq: Int) extends Response("pong")

   case class TableAdded(after_id: Option[Int], table: Table)
    extends Response("table_added")

   case class TableRemoved(id: Int) extends Response("table_removed")

   case class TableUpdated(table: Table)
    extends Response("table_updated")

//  implicit val responseDecoder: Decoder[Response] = deriveDecoder[Response]
//  implicit val responseEncoder: Encoder[Response] = deriveEncoder[Response]
}
