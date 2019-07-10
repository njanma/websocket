package njanma.entity

import njanma.dto.Request.TableRequest

case class Table(id: Option[Long],
                 name: String,
                 participants: Int,
                 ordering: Option[Long])

object Table {
  def apply(table: TableRequest): Table = apply(None, table)

  def apply(afterId: Option[Long], table: TableRequest): Table =
    new Table(table.id, table.name, table.participants, afterId)
}