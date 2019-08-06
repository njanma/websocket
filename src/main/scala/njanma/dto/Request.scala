package njanma.dto

import cats.syntax.functor._
import io.circe.generic.JsonCodec
import io.circe._
import io.circe.generic.semiauto._
import io.circe.generic.auto._, io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import njanma.dto.Request.Type.{Type, ping, remove_table, subscribe_tables, unsubscribe_tables, update_table}

sealed abstract class Request(val $type: Type)

object Request {

  @JsonCodec final case class Ping(seq: Int) extends Request(ping)

  @JsonCodec final case class SubscribeTables() extends Request(subscribe_tables)

  final case class UnsubscribeTables() extends Request(unsubscribe_tables)

  @JsonCodec final case class TableRequest(id: Option[Long],
                                     name: String,
                                     participants: Int)

  @JsonCodec final case class AddTable(after_id: Option[Long], table: TableRequest)
      extends Request(subscribe_tables)

  final case class RemoveTable(id: Long) extends Request(remove_table)

  @JsonCodec final case class UpdateTable(table: TableRequest)
      extends Request(update_table)

  implicit val decodeRequest: Decoder[Request] =
    List[Decoder[Request]](
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

  object Type extends Enumeration {
    type Type = Value
    val ping, subscribe_tables, unsubscribe_tables, add_table, remove_table,
    update_table, subscribe_changed = Value

    implicit val typeDecoder: Decoder[Type] = Decoder.enumDecoder(Type)
  }
}
