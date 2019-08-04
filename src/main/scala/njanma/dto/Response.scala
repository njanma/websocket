package njanma.dto

import njanma.dto.Request.TableRequest
import njanma.dto.Response.Type.Type
import njanma.entity.Table
import io.circe._
import io.circe.generic.semiauto._
import io.circe.generic.auto._, io.circe.syntax._

sealed abstract class Response(val $type: Type)

object Response {
  private val $type = "$type"
  private val seq = "seq"
  private val afterId = "after_id"
  private val table = "table"

  final case class Pong(seq: Int) extends Response(Type.pong)

  final case class TableResponse(id: Long, name: String, participants: Int)

  object TableResponse {
    def apply(table: Table): TableResponse =
      new TableResponse(table.id.get, table.name, table.participants)
  }

  final case class TableAdded(after_id: Option[Long], table: TableRequest)
      extends Response(Type.table_added)

  object TableAdded {
    def apply(table: Table): TableAdded = apply(None, table)

    def apply(afterId: Option[Long], table: Table): TableAdded =
      TableAdded(
        afterId,
        TableRequest(table.id, table.name, table.participants)
      )
  }

  final case class TableList(tables: List[TableResponse])
      extends Response(Type.table_list)

  final case class TableRemoved(id: Int) extends Response(Type.table_removed)

  final case class TableUpdated(table: TableRequest)
      extends Response(Type.table_removed)

  object TableUpdated {
    def apply(table: Table): TableUpdated =
      new TableUpdated(TableRequest(table.id, table.name, table.participants))
  }

  implicit val pongEncoder: Encoder[Pong] =
    Encoder.forProduct2($type, seq)(pong => (pong.$type, pong.seq))

  implicit val tableAddedEncoder: Encoder[TableAdded] =
    Encoder.forProduct3($type, afterId, table)(
      tableAdded => (tableAdded.$type, tableAdded.after_id, tableAdded.table)
    )

  implicit val tableUpdatedEncoder: Encoder[TableUpdated] =
    Encoder.forProduct2($type, table)(
      tableUpdated => (tableUpdated.$type, tableUpdated.table)
    )

  implicit val tableListEncoder: Encoder[TableList] = deriveEncoder
  implicit val tableRemovedEncoder: Encoder[TableRemoved] = deriveEncoder

  implicit val responseEncoder: Encoder[Response] = Encoder.instance {
    case pong: Pong                 => pongEncoder(pong)
    case tableAdded: TableAdded     => tableAddedEncoder(tableAdded)
    case tableUpdated: TableUpdated => tableUpdatedEncoder(tableUpdated)
    case tableList: TableList       => tableListEncoder(tableList)
    case tableRemoved: TableRemoved => tableRemovedEncoder(tableRemoved)
  }

  object Type extends Enumeration {
    type Type = Value
    val pong, table_list, table_added, table_removed = Value

    implicit val typeEncoder: Encoder[Type] = Encoder.enumEncoder(Type)
  }
}
