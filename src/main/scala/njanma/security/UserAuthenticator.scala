package njanma.security

import akka.http.scaladsl.server.directives.Credentials
import cats.Id
import njanma.entity.User
import njanma.repository.UserRepository
import org.apache.commons.codec.binary.Hex
import tsec.common._
import tsec.hashing.jca._
import doobie.implicits._

class UserAuthenticator(userRepository: UserRepository) {
  private lazy val xa = userRepository.xa

  def check(credentials: Credentials): Option[User] = {
    credentials match {
      case p@Credentials.Provided(username) =>
        userRepository
          .getOne(username)
          .transact(xa)
          .unsafeRunSync()
          .filter(u => p.verify(u.passHash, secret => Hex.encodeHexString(SHA1.hash[Id](secret.utf8Bytes))))
      case _ => None
    }
  }

  def hasPermissions(user: User): Boolean = {
    user.userType match {
      case "admin" => true
    }
  }
}
