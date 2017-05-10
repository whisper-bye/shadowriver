package im.xun.shadowriver

import java.net.InetSocketAddress
import java.nio.channels.{SelectableChannel, DatagramChannel, SocketChannel}
import ColorString._
import im.xun.shadowriver.tcpip.Protocols

import scala.concurrent.duration._
import akka.actor._
import akka.util.{Timeout, ByteString}

import java.nio.ByteBuffer


class TunLocalTcpActor(target: InetSocketAddress, server: InetSocketAddress, vpnService: ShadowRiverVpnService)
  extends Actor with ActorLogging  with Stash{

  log.info(s"TunLocalTcpActor: connect to target $target")

  val socketChannel =  SocketChannel.open()
  vpnService.protect(socketChannel.socket())
  //
  socketChannel.configureBlocking(false)

  socketChannel.connect(target)

  val readActor = context.actorOf(Props(classOf[TunTcpReadActor], socketChannel,target, context.parent), "readActor")

  def receive = preConnected

  import context.dispatcher
  context.system.scheduler.scheduleOnce(100.micro,self,"tick")
  var timeout = context.system.scheduler.scheduleOnce(10.seconds,self,"timeout")

  var i = 0
  def preConnected: Receive = {
    case "timeout" =>
      log.info(s"connection fail to established! timeout! ${target} ")
      context.stop(self)

    case "tick" =>
      i = i + 100
      if(socketChannel.finishConnect) {
        log.info(s"Socket channel Connected! use time $i micro")
        context.parent ! TunConnected
        timeout.cancel()
        context.become(postConnected)
        unstashAll()
      } else {
//        log.info("Not connected yet!")
        context.system.scheduler.scheduleOnce(100.micro,self,"tick")
      }
    case _ =>
      stash()
  }

//  def sendTcp(socketChannel: SocketChannel, data: ByteBuffer) = {
//    try{
//      socketChannel.write(data)
//    } catch {
//      case e =>
//        log.info(s"snedTcp erro reconnect: $e")
//        socketChannel.configureBlocking(true)
//        socketChannel.connect(target)
//        socketChannel.configureBlocking(false)
//        socketChannel.write(data)
//
//    }
//  }

  def postConnected: Receive = {

    case d @ TunWrite(data,buf) =>
      log.info(s"TcpTunLocalActor: TunWrite ${data.limit} bytes".blue)
      val res = socketChannel.write(data)
      assert(res == data.limit, s"Fail write all $res != ${data.limit()}")
      bufferPool.release(buf)
      readActor ! "tick"

    case msg =>
      log.error(s"TcpTunLocalActor: Unexpected message $msg".blue)
  }
}

//object TunBuffer {
//  val bufSize = 5428
//  val bufTotal = 1024
//  val bufPool = new DirectByteBufferPool(bufSize, bufTotal)
//}
class TunTcpReadActor(socketChannel: SocketChannel, target:InetSocketAddress, report: ActorRef) extends Actor with ActorLogging {
  // Allocate the buffer for a single packet.
//  val buf = ByteBuffer.allocate(32767)
//  val bufSize = 1428
//  val buf = ByteBuffer.allocate(bufSize)

  import context.dispatcher

//  context.system.scheduler.scheduleOnce(1.micro, self, "tick")

  var bufOpt: Option[ByteBuffer] = None

  def receive = {
    case "tick" =>
      if(bufOpt.isEmpty) {
        bufOpt = Some(bufferPool.acquire())
        bufOpt.get.clear()
      }

      val buf = bufOpt.get
      val res = socketChannel.read(buf)

      if(res != 0) {
        buf.flip()
        if(buf.limit > 0) {
          log.info(s"TunReadActor: 500 Read ${buf.limit()} bytes!".red)
          report ! TunRead(buf)
          bufOpt = None
        }
        context.system.scheduler.scheduleOnce(100.micro, self, "tick")
      }

      context.system.scheduler.scheduleOnce(100.micro, self, "tick")

    case msg =>
      log.error(s"TcpTunReadActor: Unexpected message $msg".cyan)
  }
}
