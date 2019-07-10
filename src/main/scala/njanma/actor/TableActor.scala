package njanma.actor

import akka.actor.{Actor, Props}
import cats.free.Free
import doobie._
import doobie.free.connection
import doobie.implicits._
import njanma.dto.Request.{AddTable, UpdateTable}
import njanma.dto.Response.TableAdded
import njanma.entity.Table
import njanma.repository.TableRepository

object TableActor {
  def props(tableRepository: TableRepository): Props =
    Props(new TableActor(tableRepository))
}

class TableActor(repository: TableRepository) extends Actor {
  private val xa = repository.xa

  override def receive: Receive = {
    case AddTable(None, table) =>
      sender() ! TableAdded(
        repository
          .insertOrUpdate(Table(table, None))
          .transact(xa)
          .unsafeRunSync()
      )
    case AddTable(Some(afterId), table) =>
      val rollBacked = Transactor.after.set(xa, HC.rollback)
      val repo = repository
      val tables = repo
        .getAllAfterId(afterId)
        .transact(xa)
        .unsafeRunSync()
      tables
        .map(t => t.copy(ordering = t.ordering.map(_ + 1)))
        .map(repo.insertOrUpdate)
        .foreach(_.transact(xa).unsafeRunSync())
      val unit: Free[connection.ConnectionOp, Table] = for {
        byAfterId <- repo.getOne(afterId)
        u <- repo.insertOrUpdate(Table(table, byAfterId.ordering.map(_ + 1)))
      } yield u
      sender() ! TableAdded(unit.transact(xa).unsafeRunSync())
    case UpdateTable(table) => repository.insertOrUpdate(Table(table, None))
  }
}
