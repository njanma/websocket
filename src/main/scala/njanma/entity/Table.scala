package njanma.entity

import njanma.dto.Request.TableRequest

case class Table(id: Option[Int],
                 name: String,
                 participants: Int,
                 ordering: Option[Int])

object Table {
  def apply(table: TableRequest, maybeAfterId: Option[Int]): Table =
    new Table(table.id, table.name, table.participants, maybeAfterId)
}