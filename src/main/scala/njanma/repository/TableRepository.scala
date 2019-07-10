package njanma.repository

import cats.effect._
import cats.implicits._
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import njanma.entity.Table

class TableRepository(connector: DbConnector) {
  val xa: Transactor[IO] = connector.xa
//  implicit val y = connector.xa.yolo // a stable reference is required

  def insertOrUpdate(table: Table): ConnectionIO[Table] = table match {
    case Table(None, name, participants, ordering) =>
      sql"""insert into "table"(name, participants, ordering)
            values($name, $participants, ${ordering.getOrElse(0)})""".update
        .withUniqueGeneratedKeys[Table](
          "id",
          "name",
          "participants",
          "ordering"
        )
    case table @ Table(Some(id), name, participants, ordering) =>
      sql"""insert into "table"(id, name, participants, ordering)
            values($id, $name, $participants, ${ordering
        .getOrElse(0)})""".update.run
      table.pure[ConnectionIO]
  }

  def getAllAfterId(afterId: Long): ConnectionIO[List[Table]] =
    sql"""select * from "table" where id >= $afterId"""
      .query[Table]
      .stream
      .compile
      .toList

  def getOne(id: Long): ConnectionIO[Table] =
    sql"""select * from "table" where id = $id""".query[Table].unique

}
