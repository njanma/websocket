package njanma.actor

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.model.ws.TextMessage
import io.circe.syntax._
import njanma.actor.SubscribingActor.{Event, Subscribe, Unsubscribe}
import njanma.dto.Response

import scala.collection.mutable.ListBuffer

object SubscribingActor {
  sealed trait Events
  final case class Subscribe(actor: ActorRef) extends Events
  final case class Unsubscribe(actor: ActorRef) extends Events
  final case class Event(changed: Response) extends Events

  def props(): Props = Props(new SubscribingActor)
}

class SubscribingActor extends Actor {
  //TODO: this stub should be replaced by shared queue
  private val subscribedActors: ListBuffer[ActorRef] = ListBuffer()

  override def receive: Receive = {
    case Subscribe(actor)   =>
      subscribedActors += actor
    case Unsubscribe(actor) => subscribedActors -= actor
    case Event(changed) =>
      subscribedActors.foreach(
        _.!(TextMessage.Strict(changed.asJson.noSpaces))
      )
  }
}
