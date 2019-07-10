package njanma.dto

import cats.syntax.functor._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.{Decoder, DecodingFailure, HCursor}

sealed abstract class Request(val $type: String)

object Request {

  case class Login(username: String, password: String) extends Request("login")

  case class Ping(seq: Int) extends Request("ping")

  case class SubscribeTables() extends Request("subscribe_tables")

  case class UnsubscribeTables() extends Request("unsubscribe_tables")

  @JsonCodec case class TableRequest(id: Option[Long], name: String, participants: Int)

  @JsonCodec case class AddTable(after_id: Option[Long], table: TableRequest)
    extends Request("add_table")

  case class RemoveTable(id: Long) extends Request("remove_table")

  @JsonCodec case class UpdateTable(table: TableRequest) extends Request("update_table")
//
//  private implicit val subscribeTablesDecoder: Decoder[Request] = Decoder.instance(getDecoderByType)
//
//  private implicit val unsubscribeTablesDecoder: Decoder[Request] = Decoder.instance(getDecoderByType)
//
  implicit val decodeRequest: Decoder[Request] =
    List[Decoder[Request]](
      Decoder[Login].widen,
      Decoder.instance(getDecoderByType),
      Decoder[Ping].widen,
      Decoder[AddTable].widen,
      Decoder[RemoveTable].widen,
      Decoder[UpdateTable].widen
    ).reduceLeft(_ or _)


  private def getDecoderByType(cursor: HCursor): Decoder.Result[Request] =
    cursor.downField("$type").as[String] match {
      case Right(t) if t.equals("subscribe_tables") =>
        Right(SubscribeTables())
      case Right(t) if t.equals("unsubscribe_tables") =>
        Right(UnsubscribeTables())
      case Right(t) if t.equals("add_table") =>
        cursor.value.as[AddTable]
      case Right(t) if t.equals("update_table") =>
        cursor.value.as[UpdateTable]
      case _ => Left(DecodingFailure("fail", Nil))
    }
}
