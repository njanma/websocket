package njanma

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}

trait WebsocketFlow {

  implicit def system: ActorSystem

  def webSocketRoute: Route = path("ws-echo")(handleWebSocketMessages(flow))

  def flow: Flow[Message, Message, NotUsed] = {
    val client = system.actorOf(Props[ClientConnectionActor])
    val sink = Sink.actorRef(client, 'sinkclose)
    val source =
      Source.actorRef(8, OverflowStrategy.fail)
        .mapMaterializedValue {
          actor =>
            client ! ('income, actor)
            actor
        }
    Flow.fromSinkAndSource(sink, source)
  }
}

class ClientConnectionActor extends Actor {
  var connection: Option[ActorRef] = None

  val receive: Receive = {
    case ('income, a: ActorRef) ⇒ connection = Some(a); context.watch(a)
    case Terminated(a) if connection.contains(a) ⇒
      connection = None; context.stop(self)
    case 'sinkclose => context.stop(self)
    case TextMessage.Strict(t) ⇒
      connection.foreach(_ ! TextMessage.Strict(s"echo $t"))
    case _ => // ignore
  }

  override def postStop(): Unit = connection.foreach(context.stop)
}
