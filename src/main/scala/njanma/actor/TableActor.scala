package njanma.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import doobie.implicits._
import njanma.dto.Request.{AddTable, RemoveTable, SubscribeTables, UpdateTable}
import njanma.dto.Response.{TableAdded, TableList, TableResponse, TableUpdated}
import njanma.entity.Table
import njanma.repository.TableRepository

import scala.language.postfixOps

object TableActor {
  def props(tableRepository: TableRepository,
            subscribingActor: ActorRef): Props =
    Props(new TableActor(tableRepository, subscribingActor))
}

class TableActor(repository: TableRepository, subscribingActor: ActorRef)
    extends Actor
    with ActorLogging {
  private val xa = repository.xa

  override def receive: Receive = {
    case AddTable(None, table) =>
      val tableAdded = TableAdded(
        repository
          .save(Table(table))
          .transact(xa)
          .unsafeRunSync()
      )
      sender() ! tableAdded
      subscribingActor ! SubscribingActor.Event(tableAdded)
    case AddTable(after_id @ Some(afterId), table) =>
      val newTable = for {
        tables <- repository.getAllAfterId(afterId)
        _ <- repository
          .update(tables.map(t => t.copy(position = t.position.map(_ + 1))))
          .compile
          .toList
        byAfterId <- repository.getOne(afterId)
        newbie <- repository.save(Table(byAfterId.position.map(_ + 1), table))
      } yield newbie
      val tableAdded =
        TableAdded(after_id, newTable.transact(xa).unsafeRunSync())
      sender() ! tableAdded
      subscribingActor ! SubscribingActor.Event(tableAdded)
    case UpdateTable(table) =>
      val tableUpdated = TableUpdated(
        repository.update(Table(table)).transact(xa).unsafeRunSync()
      )
      sender() ! tableUpdated
      subscribingActor ! SubscribingActor.Event(tableUpdated)
    case RemoveTable(id) =>
      repository.delete(id).transact(xa).unsafeRunSync()
  }
}
