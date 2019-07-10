package njanma.repository

import cats.effect._
import cats.implicits._
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import njanma.entity.Table

class TableRepository(connector: DbConnector) {
  val xa: Transactor[IO] = connector.xa

  def insertOrUpdate(table: Table): ConnectionIO[Table] = table match {
    case Table(None, name, participants, ordering) =>
      sql"""insert into "table"(name, participants, ordering)
            values($name, $participants, $ordering)"""
        .updateWithLogHandler(LogHandler.jdkLogHandler)
        .withUniqueGeneratedKeys[Table](
          "id",
          "name",
          "participants",
          "ordering"
        )
    case table @ Table(Some(id), name, participants, ordering) =>
      sql"""insert into "table"(id, name, participants, ordering)
            values($id, $name, $participants, $ordering)""".updateWithLogHandler(LogHandler.jdkLogHandler).run
      table.pure[ConnectionIO]
  }

  def getAllAfterId(afterId: Long): ConnectionIO[List[Table]] =
    sql"""select * from "table" where id > $afterId"""
      .queryWithLogHandler[Table](LogHandler.jdkLogHandler)
      .stream
      .compile
      .toList

  def getOne(id: Long): ConnectionIO[Table] =
    sql"""select * from "table" where id = $id"""
      .queryWithLogHandler[Table](LogHandler.jdkLogHandler)
      .unique

}
