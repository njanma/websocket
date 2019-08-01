package njanma

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.ActorMaterializer
import akka.testkit.TestActors
import njanma.security.UserAuthentificator
import org.scalatest.{Matchers, WordSpec}

class WebSocketSecuritySpec extends WordSpec with Matchers with ScalatestRouteTest {
  private val testMaterializer = this.materializer
  private val route = new WebSocketFlow {
    override implicit def materializer: ActorMaterializer = testMaterializer
    override def tableActor: ActorRef = system.actorOf(TestActors.echoActorProps)

    override def userAuthenticator: UserAuthentificator = ???
  }.webSocketRoute

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
