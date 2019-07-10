package njanma.actor

import akka.actor.{Actor, Props}
import doobie._
import doobie.implicits._
import njanma.dto.Request.{AddTable, UpdateTable}
import njanma.entity.Table
import njanma.repository.TableRepository

class TableActor(repository: TableRepository) extends Actor {

  override def receive: Receive = {
    case AddTable(None, table) =>
      sender() ! repository.insertOrUpdate(Table(table, None)).transact(repository.xa).unsafeRunSync()
    case AddTable(Some(afterId), table) =>
      val xa = Transactor.after.set(repository.xa, HC.rollback)
      for {
        table <- repository.getAllAfterId(afterId).transact(xa).unsafeRunSync()
      } yield
        repository
          .insertOrUpdate(table.copy(ordering = table.ordering.map(_ + 1)))
          .transact(xa)
          .unsafeRunSync()
      for {
        byAfterId <- repository.getOne(afterId)
      } yield
        repository
          .insertOrUpdate(Table(table, byAfterId.ordering.map(_ + 1)))
          .transact(xa)
          .unsafeRunSync()
    case UpdateTable(table) => repository.insertOrUpdate(Table(table, None))
  }
}

object TableActor {
  def props(tableRepository: TableRepository): Props =
    Props(new TableActor(tableRepository))
}
