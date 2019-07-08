package njanma

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import njanma.UserActor.{CreateUser, GetUsers}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import njanma.db.UserRepositoryActor.{GetAll, Save}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._

case class User(id: Option[Int], name: String)

object UserActor {
  final case object GetUsers
  final case class CreateUser(user: User)

  def props(userRepository: ActorRef): Props =
    Props(new UserActor(userRepository))
}

class UserActor(userRepository: ActorRef) extends Actor with ActorLogging {

  implicit lazy val timeout: Timeout = Timeout(5 seconds)

  override def receive: Receive = {
    case GetUsers =>
      (userRepository ? GetAll).pipeTo(sender())
    case CreateUser(user) =>
      (userRepository ? Save(user)).pipeTo(sender())
  }
}
