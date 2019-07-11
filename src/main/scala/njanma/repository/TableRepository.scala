package njanma.repository

import cats.effect._
import cats.implicits._
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import fs2.Stream
import njanma.entity.Table

class TableRepository(connector: DbConnector) {
  val xa: Transactor[IO] = connector.xa

  def save(table: Table): ConnectionIO[Table] = table match {
    case Table(_, name, participants, None) =>
      insertOrUpdate(sql"""insert into "table"(name, participants)
              values($name, $participants)""")
    case Table(_, name, participants, Some(ord)) =>
      insertOrUpdate(sql"""insert into "table"(name, participants, ordering)
              values($name, $participants, $ord)""")
  }
  def update(tables: List[Table]): Stream[doobie.ConnectionIO, Table] = {
    Param
    Update[Table](s"""update "table"
            set name=?, participants=?, ordering=?
            where id = ?""")
      .updateManyWithGeneratedKeys[Table](
        "id",
        "name",
        "participants",
        "ordering"
      )(tables)
  }

  def update(table: Table): ConnectionIO[Table] = table match {
    case Table(Some(id), name, participants, Some(ord)) =>
      insertOrUpdate(sql"""update "table"
            set name=$name, participants=$participants, ordering=$ord
            where id = $id""")
    case Table(Some(id), name, participants, None) =>
      insertOrUpdate(sql"""update "table"
            set name=$name, participants=$participants
            where id = $id""")
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

  def getAll(): ConnectionIO[List[Table]] =
    sql"""select * from "table" order by ordering"""
      .query[Table]
      .stream
      .compile
      .toList

  private def insertOrUpdate(sql: Fragment): ConnectionIO[Table] =
    sql
      .updateWithLogHandler(LogHandler.jdkLogHandler)
      .withUniqueGeneratedKeys[Table]("id", "name", "participants", "ordering")
}
