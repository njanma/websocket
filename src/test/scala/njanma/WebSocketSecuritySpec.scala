package njanma

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.testkit.TestActors
import njanma.dto.Request.Ping
import njanma.entity.User
import njanma.repository.{DbConnector, UserRepository}
import njanma.security.UserAuthenticator
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpec}

class WebSocketSecuritySpec extends WordSpec
          with Matchers
          with ScalatestRouteTest
          with MockFactory
          with BeforeAndAfterAll {
  private val testMaterializer = this.materializer
  private val fakeAuthenticator = stub[FakeAuth]
  private val route = new WebSocketFlow {
    override implicit def materializer: ActorMaterializer = testMaterializer
    override def tableActor: ActorRef = system.actorOf(TestActors.echoActorProps)
    override def userAuthenticator: UserAuthenticator = fakeAuthenticator
    override val connectionPath: String = "connect"
  }.webSocketRoute


  class FakeRepository extends UserRepository(mock[DbConnector])
  class FakeAuth extends UserAuthenticator(mock[FakeRepository])

  override protected def beforeAll(): Unit = {
    val fakeUser = User("john", "p4ssw0rd", "admin")
    (fakeAuthenticator.check _)
      .when(Credentials(Some(BasicHttpCredentials("john", "p4ssw0rd"))))
      .returns(Option(fakeUser))
    (fakeAuthenticator.hasPermissions _)
      .when(*)
      .returns(true)
  }

  "WebSocketFlow" must {
    "reject users without auth" in {
      val wsClient = WSProbe()
      WS("/connect", wsClient.flow) ~> route ~>
        check(
          rejection shouldBe a[AuthenticationFailedRejection]
        )
    }
    "accept users with Basic Auth" in {
      val wsClient = WSProbe()
      WS("/connect", wsClient.flow).addCredentials(BasicHttpCredentials("john", "p4ssw0rd")) ~> route ~>
        check {
          isWebSocketUpgrade should be(true)
          status should be(StatusCodes.SwitchingProtocols)
        }
    }
  }
}