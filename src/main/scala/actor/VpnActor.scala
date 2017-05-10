package im.xun.shadowriver

import java.io.{FileOutputStream, FileDescriptor}
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.Random
import ColorString._
import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.actor._
import im.xun.shadowriver.tcpip._

case class TcpRead(pkg: TcpPackage, buf: ByteBuffer)
case class TcpWrite(pkg: TcpPackage, buf: ByteBuffer)
case class UdpRead(pkg: UdpPackage)
case class UdpWrite(pkg: UdpPackage)

class VpnActor(netFd: FileDescriptor, vpnService: ShadowRiverVpnService) extends Actor  {
  log.info("VpnActor: Start!")
  import context.dispatcher

  lazy val out = new FileOutputStream(netFd).getChannel
  //val server = new InetSocketAddress("localhost",8080)
  val server = new InetSocketAddress("192.168.0.12",8080)

//  context.system.scheduler.scheduleOnce(5.seconds, self, "alive")

  val connectionMap = new java.util.concurrent.ConcurrentHashMap[TcpConnectionTuple, ActorRef]()

  def removeValue[K,V](map: ConcurrentHashMap[K,V], value: V) = {
    for(key <- map.keys){
      if(map.get(key) == value) {
        log.info(s"Remove TcpConnectionTuple:$key from connMap size: ${connectionMap.size()}")
        map.remove(key)
      }
    }
  }

  def findConnectionActor(tuple: TcpConnectionTuple): ActorRef = {
    if(connectionMap.get(tuple) == null) {
      val app: Option[AppInfo] = AppInfoUtils.appForTupleUpdate(tuple)

      val appName = if(app.nonEmpty) {
        app.get.appName.replaceAll(" +","-") + scala.util.Random.nextInt
      } else {
        "Unknow-App" + scala.util.Random.nextInt
      }

      log.info(s"VpnActor: Create connection actor for $appName ${tuple}".green)

      val conn = if(tuple.protocol == PROTOCOL_TCP) {
        context.actorOf(Props(classOf[TcpConnectionActor],tuple, self,server,vpnService),s"tcpConnectionActor-$appName")
      } else {
        context.actorOf(Props(classOf[UdpConnectionActor],tuple, self,server,vpnService),s"udpConnectionActor-$appName")
      }

      context.watch(conn)
      connectionMap.put(tuple,conn)
    }
    connectionMap.get(tuple)
  }

  val readActor = context.actorOf(Props(classOf[NetIOReadActor],netFd,self),"read")

  //receive pkg from vpn interface, send pkg to vpn interface
  def receive = {
    case Terminated(child) =>
      log.info(s"connection actor $child terminated!".green)
      removeValue(connectionMap, child)

    case UdpRead(pkg) =>
      val tuple = TcpConnectionTuple(pkg.ipHeader.sourceIp,pkg.ipHeader.destinationIp,
        pkg.udpHeader.sourcePort,pkg.udpHeader.destinationPort, PROTOCOL_UDP)
      log.info(s"VpnActor: Receive udp package from ${AppInfoUtils.appForTuple(tuple).map(_.pkgName)} dst:${pkg.ipHeader.destinationIp} size:${pkg.udpHeader.length}".green)
      findConnectionActor(tuple) ! pkg

    case TcpRead(pkg,buf) =>
      val tuple = TcpConnectionTuple(pkg.ipHeader.sourceIp,pkg.ipHeader.destinationIp,
        pkg.tcpHeader.sourcePort,pkg.tcpHeader.destinationPort, PROTOCOL_TCP)
      log.info(s"VpnActor: Receive tcp package from ${AppInfoUtils.appForTuple(tuple).map(_.pkgName)}  dst:${pkg.ipHeader.destinationIp} size:${pkg.payloadLen} ${pkg.tcpHeader.flags}".green)
      findConnectionActor(tuple) ! TcpRead(pkg,buf)

    case TcpWrite(pkg,buf) =>
      log.info(s"VpnActor: NetIOWrite tcp  ${pkg.payload.length}".green)
//      log.info(s"VpnActor: Send tcp package src:${pkg.ipHeader.sourceIp} size:${pkg.payloadLen} ${pkg.tcpHeader.flags}".green)
      out.write(pkg)
      if(buf!= null) {
        bufferPool.release(buf)
      }

    case UdpWrite(pkg) =>
      log.info(s"VpnActor:NetIOWrite udp ${pkg.payload.length}".green)
//      log.info(s"VpnActor: Receive package dst:${pkg.ipHeader.destinationIp} size:${pkg.udpHeader.length}".green)
//      Utils.dumpBytes(pkg)
      out.write(pkg)
    case msg =>
      log.error(s"VpnActor: Unkonwn msg:$msg")


  }

}
