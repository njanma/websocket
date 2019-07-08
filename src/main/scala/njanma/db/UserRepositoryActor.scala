package njanma.db

import akka.actor.{Actor, Props}
import doobie._
import doobie.implicits._
import njanma.User
import njanma.db.UserRepositoryActor.{GetAll, GetById, Save}

object UserRepositoryActor {
  case object GetAll
  case class GetById(id: Int)
  case class Save(user: User)

  def props(connector: DbConnector): Props =
    Props(new UserRepositoryActor(connector))
}

class UserRepositoryActor(connector: DbConnector) extends Actor {
  val y = connector.xa.yolo // a stable reference is required

  override def receive: Receive = {
    case GetAll =>
      sender() ! all().transact(connector.xa).unsafeRunSync()
    case GetById(id) =>
      sender() ! get(id).transact(connector.xa).unsafeRunSync()
    case Save(user) =>
      sender() ! save(user).transact(connector.xa).unsafeRunSync()
  }

  def all(): ConnectionIO[List[User]] =
    sql"""select * from "user" """
      .query[User]
      .stream
      .compile
      .toList

  def get(id: Int): ConnectionIO[Option[User]] =
    sql"""select * from "user" where id = $id""".query[User].option

  def save(user: User): ConnectionIO[User] = user match {
    case User(None, name) =>
      sql"""insert into "user"(name) values($name)""".update
        .withUniqueGeneratedKeys("id", "name")
  }
}
