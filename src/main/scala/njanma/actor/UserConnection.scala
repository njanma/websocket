package njanma.actor

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import njanma.actor.SubscribingActor.{Subscribe, Unsubscribe}
import njanma.actor.UserConnection.Connected
import njanma.dto.Request.{
  AddTable,
  Ping,
  RemoveTable,
  SubscribeTables,
  UnsubscribeTables,
  UpdateTable
}
import njanma.dto.Response.Pong
import njanma.dto.{Request, Response}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._

object UserConnection {
  case class Connected(user: ActorRef)
  case class In(text: Request)
  case class Out(text: Response)

  def props(tableActor: ActorRef, subscribingActor: ActorRef): Props =
    Props(new UserConnection(tableActor, subscribingActor))
}

class UserConnection(tableActor: ActorRef, subscribingActor: ActorRef)
    extends Actor
    with ActorLogging {

  implicit lazy val timeout: Timeout = Timeout(10 second)

  override def receive: Receive = waiting

  def waiting: Receive = {
    case Connected(user) =>
      log.info(s"User: $user has connected")
      context become connected(user)
  }

  def connected(user: ActorRef): Receive = {
    case Ping(num) =>
      user ! Pong(num)
    case addTable: AddTable =>
      (tableActor ? addTable).pipeTo(user)
    case updateTable: UpdateTable =>
      (tableActor ? updateTable).pipeTo(user)
    case SubscribeTables() =>
      subscribingActor ! Subscribe(user)
    case UnsubscribeTables() =>
      subscribingActor ! Unsubscribe(user)
    case removeTable: RemoveTable =>
      (tableActor ? removeTable).pipeTo(user)
    case PoisonPill =>
      subscribingActor ! Unsubscribe(user)
  }
}
