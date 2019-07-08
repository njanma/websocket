package njanma.db

import cats.effect.IO
import cats.implicits._
import doobie.Transactor
import doobie.implicits._

import scala.concurrent.ExecutionContext

class DbConnector(val xa: Transactor[IO])

object DbConnector {
  def apply(): DbConnector = {
    implicit val cs = IO.contextShift(ExecutionContext.global)
    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:users",
      "postgres",
      "secret"
    )
    val setSchema = sql"set SEARCH_PATH = public".update.run.void
    new DbConnector(Transactor.before.modify(xa, _ *> setSchema))
  }
}
