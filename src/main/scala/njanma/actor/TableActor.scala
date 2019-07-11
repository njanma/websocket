package njanma.actor

import akka.actor.{Actor, ActorLogging, Props}
import doobie.implicits._
import doobie.util.update.Update
import njanma.dto.Request.{AddTable, UpdateTable}
import njanma.dto.Response.{TableAdded, TableUpdated}
import njanma.entity.Table
import njanma.repository.TableRepository

import scala.language.postfixOps

object TableActor {
  def props(tableRepository: TableRepository): Props =
    Props(new TableActor(tableRepository))
}

class TableActor(repository: TableRepository) extends Actor with ActorLogging {
  private val xa = repository.xa

  override def receive: Receive = {
    case AddTable(None, table) =>
      sender() ! TableAdded(
        repository
          .save(Table(table))
          .transact(xa)
          .unsafeRunSync()
      )
    case AddTable(after_id @ Some(afterId), table) =>
      val newTable = for {
        tables <- repository.getAllAfterId(afterId)
        _ <- repository
          .update(tables.map(t => t.copy(ordering = t.ordering.map(_ + 1))))
          .compile
          .toList
        byAfterId <- repository.getOne(afterId)
        newbie <- repository.save(Table(byAfterId.ordering.map(_ + 1), table))
      } yield newbie
      sender() ! TableAdded(after_id, newTable.transact(xa).unsafeRunSync())
    case UpdateTable(table) =>
      sender() ! TableUpdated(
        repository.update(Table(table)).transact(xa).unsafeRunSync()
      )
  }
}
