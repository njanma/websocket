package njanma.repository

import cats.effect._
import cats.implicits._
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import fs2.Stream
import njanma.entity.Table
import njanma.repository.TableRepository.SQL

class TableRepository(connector: DbConnector) {
  type TableUpd = (String, Int, Long, Long)

  val xa: Transactor[IO] = connector.xa

  def save(table: Table): ConnectionIO[Table] =
    insertOrUpdate(SQL.insert(table))

  def update(table: Table): ConnectionIO[Table] =
    insertOrUpdate(SQL.update(table))

  def update(tables: List[Table]): Stream[doobie.ConnectionIO, Table] =
    Update[TableUpd](s"""update tables
            set name=?, participants=?, position=?
            where id = ?""")
      .updateManyWithGeneratedKeys[Table](
        "id",
        "name",
        "participants",
        "position"
      )(
        tables.map(
          table =>
            (
              table.name,
              table.participants,
              table.position.getOrElse(0),
              table.id.get
            ): TableUpd
        )
      )

  def getAllAfterId(afterId: Long): ConnectionIO[List[Table]] =
    sql"""select * from tables where id > $afterId"""
      .queryWithLogHandler[Table](LogHandler.jdkLogHandler)
      .stream
      .compile
      .toList

  def getOne(id: Long): ConnectionIO[Table] =
    sql"""select * from tables where id = $id"""
      .queryWithLogHandler[Table](LogHandler.jdkLogHandler)
      .unique

  def getAll(): ConnectionIO[List[Table]] =
    sql"""select * from tables order by position"""
      .query[Table]
      .stream
      .compile
      .toList

  private def insertOrUpdate(sql: Fragment): ConnectionIO[Table] =
    sql
      .updateWithLogHandler(LogHandler.jdkLogHandler)
      .withUniqueGeneratedKeys[Table]("id", "name", "participants", "position")
}

object TableRepository {

  object SQL {
    def update(table: Table): Fragment = table match {
      case Table(Some(id), name, participants, maybePos) =>
        sql"""update tables
            set name=$name, participants=$participants""" ++ maybePos.map(ord => fr", position=$ord").getOrElse(fr"") ++
          fr"where id = $id"
    }

    def insert(table: Table): Fragment = table match {
      case Table(_, name, participants, maybePos) =>
        sql"""insert into tables(name, participants""" ++ maybePos.map(_ => fr" ,position)").getOrElse(fr")") ++
          fr" values($name, $participants" ++ maybePos.map(ord => fr", $ord)").getOrElse(fr")")
    }
  }

}
