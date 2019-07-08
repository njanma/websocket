package njanma

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import njanma.UserActor.{CreateUser, GetUsers}

import scala.concurrent.duration._

trait UserRoutes {
  implicit def system: ActorSystem

  implicit lazy val timeout: Timeout = Timeout(5 seconds)

  def userActor: ActorRef

  lazy val userRoutes: Route =
    pathPrefix("users") {
      get {
        onSuccess((userActor ? GetUsers).mapTo[List[User]]) { users =>
          complete(users)
        }
      } ~ (post & entity(as[User])) { user =>
        onSuccess((userActor ? CreateUser(user)).mapTo[User]) { usr =>
          complete(usr)
        }
      }
    }
}
