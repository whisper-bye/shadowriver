package im.xun.shadowriver

import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import ColorString._
import im.xun.shadowriver.tcpip.Protocols

import scala.concurrent.duration._
import akka.actor._
import akka.util.{Timeout, ByteString}

import java.nio.ByteBuffer

case class TunWrite(payload: ByteBuffer, buf: ByteBuffer )

case class TunRead(payload: ByteBuffer)

case object TunConnected

case object TunEstablished

case object TunWorkGet

case class TunWorkConfig(target: InetSocketAddress, workFor: ActorRef)

case class TunWorker(worker: ActorRef)

class TunActor(target: InetSocketAddress, server: InetSocketAddress, protocol: Int, vpnService: ShadowRiverVpnService) extends Actor with ActorLogging {

  var tunWorker: Option[ActorRef] = None

  def createWorker = if (protocol == Protocols.Tcp){
    log.info(s"TCP: spawn TunLocalTcpActor worker for $target")
    context.actorOf(Props(classOf[TunLocalTcpNioActor], target, server, vpnService), "tunWorkerTcp")
  } else {
    log.info(s"UDP: spawn TunLocalUdpActor worker for $target")
    context.actorOf(Props(classOf[TunLocalUdpActor], target, server, vpnService), "tunWorkerUdp")
  }

  def getWorker = {
    if(tunWorker.nonEmpty) {
      tunWorker.get
    } else {
      val worker = createWorker
      context.watch(worker)
      tunWorker=Some(worker)
      worker
    }
  }

  getWorker

  var report: ActorRef = _
  def receive = {
    case TunConnected =>
      context.parent ! TunConnected

    case Terminated(child) =>
      log.info("Worker Die!")
      tunWorker = None

    case d@TunWrite(payload, buf) =>
      log.info(s"TunActor: TunWrite ${payload.limit()} bytes".blue)
      report = sender()
      getWorker ! d

    case d@TunRead(data) =>
      log.info(s"TunActor: TunRead ${data.limit()} bytes".blue)
      report ! d

    case msg =>
      log.error(s"TcpTunActor: Unexpected message $msg".blue)
  }
}
