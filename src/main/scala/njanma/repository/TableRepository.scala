package njanma.repository

import cats.effect.IO
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import njanma.entity.Table

class TableRepository(connector: DbConnector) {
  val xa: Transactor[IO] = connector.xa
//  implicit val y = connector.xa.yolo // a stable reference is required

  def insertOrUpdate(table: Table): ConnectionIO[Int] = table match {
    case Table(None, name, participants, ordering) =>
      sql"""insert into "table"(name, participants, ordering) values($name, $participants)""".update
        .withUniqueGeneratedKeys("id")
    case Table(Some(id), name, participants, ordering) =>
      sql"""insert into "table"(id, name, participants) values($id, $name, participants, ordering)""".update.run
  }

  def getAllAfterId(afterId: Long): ConnectionIO[List[Table]] =
    sql"""select * from table where id >= $afterId"""
      .query[Table]
      .stream
      .compile
      .toList

  def getOne(id: Long): ConnectionIO[Table] =
    sql"""select * from table where id = $id""".query[Table].unique

}
