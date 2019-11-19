package njanma.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import cats.effect.IO
import doobie.implicits._
import njanma.dto.Request.{AddTable, RemoveTable, UpdateTable}
import njanma.dto.Response.{TableAdded, TableUpdated}
import njanma.entity.Table
import njanma.repository.TableRepository

import scala.concurrent.ExecutionContext.Implicits._
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
      repository
        .save(Table(table))
        .transact(xa)
        .unsafeToFuture()
        .map(TableAdded(_))
        .foreach { tableAdded =>
          sender() ! tableAdded
          subscribingActor ! SubscribingActor.Event(tableAdded)
        }

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
      newTable
        .transact(xa)
        .unsafeToFuture()
        .map(TableAdded(after_id, _))
        .foreach { tableAdded =>
          sender() ! tableAdded
          subscribingActor ! SubscribingActor.Event(tableAdded)
        }

    case UpdateTable(table) =>
      repository
        .update(Table(table))
        .transact(xa)
        .unsafeToFuture()
        .map(TableUpdated(_))
        .foreach { table =>
          sender() ! table
          subscribingActor ! SubscribingActor.Event(table)
        }

    case RemoveTable(id) =>
      repository
        .delete(id)
        .transact(xa)
        .runAsync {
          case Left(ex) => IO(ex.printStackTrace())
          case Right(_) => IO.unit
        }
  }
}
