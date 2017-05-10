package im.xun.shadowriver

import java.net.{DatagramPacket, DatagramSocket, InetSocketAddress}
import java.nio.channels.{SelectableChannel, DatagramChannel, SocketChannel}
import ColorString._
import im.xun.shadowriver.tcpip.Protocols

import scala.concurrent.duration._
import akka.actor._
import akka.util.{Timeout, ByteString}

import java.nio.ByteBuffer


class TunLocalUdpActor(target: InetSocketAddress, server: InetSocketAddress,  vpnService: ShadowRiverVpnService)
  extends Actor
  with Stash
  with ActorLogging {

  log.info(s"TunLocalUdpActor: connect to target $target")
  import context.dispatcher

  val udpChannel = DatagramChannel.open()
  vpnService.protect(udpChannel.socket())
  udpChannel.configureBlocking(false)
  udpChannel.connect(target)

  val readActor = context.actorOf(Props(classOf[TunUdpReadActor], udpChannel, context.parent), "readActor")

//  var test = false
//  def receive2: Receive = {
//    case d@TunWrite(data) =>
//      log.info(s"UdpTunLocalActor: TunWrite ${data.length} bytes".blue)
////      if(!udpChannel.isConnected) {
//      if(!test){
//        test = true
//        log.info(s"UdpTunLocalActor not connected yet!")
//        context.become({
//          case "tick" =>
//            log.info("tick")
//            if(udpChannel.isConnected) {
//              log.info(s"UdpTunLocalActor connected!")
//              context.unbecome()
//              unstashAll()
//            } else {
//              context.system.scheduler.scheduleOnce(10.micro, self, "tick")
//            }
//          case _ =>
//            log.info("tick: stash")
//            stash()
//        },discardOld = false)
//        stash()
//        self ! "tick"
//
//      } else {
//        udpChannel.write(data.asByteBuffer)
//        readActor ! "tick"
//      }
//
//    case msg =>
//      log.error(s"UdpTunLocalActor: Unexpected message $msg".blue)
//  }

  def receive = preConnected

  context.system.scheduler.scheduleOnce(100.micro,self,"tick")

  def preConnected: Receive = {
    case "tick" =>
      if(udpChannel.isConnected) {
        log.info("Socket channel Connected!")
        context.become(postConnected)
        unstashAll()
      } else {
        log.info("Not connected yet!")
        context.system.scheduler.scheduleOnce(10.micro,self,"tick")
      }
    case _ =>
      stash()
  }

  def postConnected: Receive = {

    case d@TunWrite(data,buf) =>
      //log.info(s"UdpTunLocalActor: TunWrite ${data.length} bytes".blue)
      udpChannel.write(data)
      readActor ! "tick"

    case "timeout" =>
      log.info("TunLocalUdpActor: Not data read in 5 seconds udpChannel timeout! Bye!")
      context.stop(self)

    case msg =>
      log.error(s"UdpTunLocalActor: Unexpected message $msg".blue)
  }
}


class TunUdpReadActor(udpChannel: DatagramChannel, report: ActorRef) extends Actor with ActorLogging {
  // Allocate the buffer for a single packet.
  val buf = ByteBuffer.allocate(1024)
//  val buf = new Array[Byte](1024)
//  val pkg = new DatagramPacket(buf , buf.length)

  import context.dispatcher

  var timeout = context.system.scheduler.scheduleOnce(15.second, self, "timeout")

  def receive = {
    case "tick" =>
      val res = udpChannel.read(buf)
      if(res != 0) {
        timeout.cancel()
        timeout = context.system.scheduler.scheduleOnce(15.second, self, "timeout")
        buf.flip()
        log.info(s"TunReadActor: Read ${buf.limit()} bytes!".red)
        report ! TunRead(buf)
        context.system.scheduler.scheduleOnce(100.micro, self, "tick")
      } else {
        context.system.scheduler.scheduleOnce(100.micro, self, "tick")
      }

    case "timeout" =>
      log.info("TunUdpReadActor: Not data read in 5 seconds udpChannel timeout! Bye!")
      context.parent ! "timeout"
      context.stop(self)

    case msg =>
      log.error(s"TcpTunReadActor: Unexpected message $msg".cyan)
  }
}
