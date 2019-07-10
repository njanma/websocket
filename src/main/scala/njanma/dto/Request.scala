package njanma.dto

import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.{Decoder, DecodingFailure}

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

  private val subscribeTablesDecoder: Decoder[Request] = Decoder.instance {
    cursor =>
      cursor.downField("$type").as[String] match {
        case Right(t) if t.equals("subscribe_tables") =>
          Right(SubscribeTables())
        case _ => Left(DecodingFailure("fail", Nil))
      }
  }

  private val unsubscribeTablesDecoder: Decoder[Request] = Decoder.instance {
    cursor =>
      cursor.downField("$type").as[String] match {
        case Right(t) if t.equals("unsubscribe_tables") =>
          Right(UnsubscribeTables())
        case _ => Left(DecodingFailure("fail", Nil))
      }
  }
  implicit val decodeEvent: Decoder[Request] =
    List[Decoder[Request]](
      Decoder[Login].widen,
      subscribeTablesDecoder,
      unsubscribeTablesDecoder,
      Decoder[Ping].widen,
      Decoder[AddTable].widen,
      Decoder[RemoveTable].widen,
      Decoder[UpdateTable].widen
    ).reduceLeft(_ or _)
}
