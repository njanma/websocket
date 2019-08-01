package njanma.repository

import cats.effect._
import cats.implicits._
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import njanma.entity.User

class UserRepository(connector: DbConnector) {
  val xa: Transactor[IO] = connector.xa

  def getOne(username: String): ConnectionIO[Option[User]] =
    sql"""select *
         from users
         where username = $username"""
    .query[User]
    .option
}