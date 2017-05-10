//package im.xun.shadowriver
//
//import java.net.InetSocketAddress
//import java.nio.channels.{SelectableChannel, DatagramChannel, SocketChannel}
//import ColorString._
//import im.xun.shadowriver.tcpip.Protocols
//
//import scala.concurrent.duration._
//import akka.actor.{Props, ActorLogging, ActorRef, Actor}
//import akka.util.{Timeout, ByteString}
//
//import java.nio.ByteBuffer
//
//
//class TunLocalActor(target: InetSocketAddress, server: InetSocketAddress, protocol: Int, vpnService: ShadowRiverVpnService) extends Actor with ActorLogging {
//
//  log.info(s"TunLocalActor: connect to target $target")
//
////  var tcpChannel: SocketChannel = _
////  var udpChannel: DatagramChannel = _
////  var readActor: ActorRef = _
////
////  if(protocol == Protocols.Tcp) {
////    tcpChannel = SocketChannel.open()
////    vpnService.protect(tcpChannel.socket())
////    tcpChannel.connect(target)
////    log.info(s"TunLocalActor: tcpChannel connected!")
////    tcpChannel.configureBlocking(false)
////    readActor = context.actorOf(Props(classOf[TunTcpReadActor],tcpChannel,context.parent),"readActor")
////  }
////
////  if(protocol == Protocols.Udp) {
////    udpChannel = DatagramChannel.open()
////    vpnService.protect(tcpChannel.socket())
////    udpChannel.connect(target)
////    log.info(s"TunLocalActor: UdpChannel connected!")
////    udpChannel.configureBlocking(false)
////    readActor = context.actorOf(Props(classOf[TunUdpReadActor],udpChannel,context.parent),"readActor")
////  }
//
////
////  def socketWrite(data: ByteBuffer) = {
////    if(protocol == Protocols.Tcp) {
////      tcpChannel.write(data)
////    } else {
////      udpChannel.write(data)
////    }
////  }
//
//
//  val socketChannel =  SocketChannel.open()
//  vpnService.protect(socketChannel.socket())
////
//  socketChannel.connect(target)
//  log.info(s"TunLocalActor: connected!")
//  socketChannel.configureBlocking(false)
//
//
//
//  def receive = {
//
//    case d @ TunWrite(data) =>
//      log.info(s"TcpTunLocalActor: TunWrite ${data.length} bytes".blue)
//      socketChannel.write(data.asByteBuffer)
//
//    case msg =>
//      log.error(s"TcpTunLocalActor: Unexpected message $msg".blue)
//  }
//}
//
//class TunTcpReadActor(socketChannel: SocketChannel, report: ActorRef) extends Actor with ActorLogging {
//  // Allocate the buffer for a single packet.
//  val buf = ByteBuffer.allocate(32767)
//
//  import context.dispatcher
//
//  context.system.scheduler.scheduleOnce(1.micro, self, "tick")
//
//  def receive = {
//    case "tick" =>
//      buf.clear()
//      val res = socketChannel.read(buf)
//      if(res != 0) {
//        buf.flip()
//        if(buf.limit > 0) {
//          log.info(s"TunReadActor: Read ${buf.limit()} bytes!".red)
//          report ! TunRead(ByteString(buf))
//        }
//      }
//      context.system.scheduler.scheduleOnce(100.micro, self, "tick")
//
//    case msg =>
//      log.error(s"TcpTunReadActor: Unexpected message $msg".cyan)
//  }
//}
//
////class TunUdpReadActor(socketChannel: DatagramChannel, report: ActorRef) extends Actor with ActorLogging {
////  // Allocate the buffer for a single packet.
////  val buf = ByteBuffer.allocate(32767)
////
////  import context.dispatcher
////
////  context.system.scheduler.scheduleOnce(1.micro, self, "tick")
////
////  def receive = {
////    case "tick" =>
////      buf.clear()
////      socketChannel.receive(buf);
////      val res = socketChannel.read(buf)
////      if(res != 0) {
////        buf.flip()
////        log.info(s"TunReadActor: Read ${buf.limit()} bytes!".red)
////        report ! TunRead(ByteString(buf))
////      }
////      context.system.scheduler.scheduleOnce(100.micro, self, "tick")
////
////    case msg =>
////      log.error(s"TcpTunReadActor: Unexpected message $msg".cyan)
////  }
////}
