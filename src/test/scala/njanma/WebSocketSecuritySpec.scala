package njanma

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.ActorMaterializer
import akka.testkit.TestActors
import njanma.entity.User
import njanma.repository.UserRepository
import njanma.security.UserAuthenticator
import org.scalamock.scalatest.MockFactory
import org.scalatest._

class WebSocketSecuritySpec
    extends WordSpec
    with Matchers
    with ScalatestRouteTest
    with MockFactory
    with OneInstancePerTest {
  private val testMaterializer = this.materializer
  private val fakeAuth = stub[FakeAuth]
  private val route = new WebSocketFlow {
    override implicit def materializer: ActorMaterializer = testMaterializer
    override def tableActor: ActorRef =
      system.actorOf(TestActors.echoActorProps)
    override def userAuthenticator: UserAuthenticator = fakeAuth
    override val connectionPath: String = "connect"
  }.webSocketRoute
  class FakeAuth extends UserAuthenticator(mock[UserRepository])
  val fakeUser = User("john", "p4ssw0rd", "admin")

  (fakeAuth.check _)
    .when(Credentials(Some(BasicHttpCredentials("john", "p4ssw0rd"))))
    .returns(Some(fakeUser))
    .anyNumberOfTimes()
  (fakeAuth.hasPermissions _).when(*).returns(true).anyNumberOfTimes()
  (fakeAuth.check _).when(Credentials.Missing).returns(None).anyNumberOfTimes()

  "WebSocketFlow" must {
    "reject users without auth" in {
      val wsClient = WSProbe()
      WS("/connect", wsClient.flow) ~> route ~>
        check {
          rejection shouldBe a[AuthenticationFailedRejection]
        }
    }
    "accept users with Basic Auth" in {
      val wsClient = WSProbe()
      WS("/connect", wsClient.flow)
        .addCredentials(BasicHttpCredentials("john", "p4ssw0rd")) ~> route ~>
        check {
          isWebSocketUpgrade should be(true)
          status should be(StatusCodes.SwitchingProtocols)
        }
    }
  }
}
