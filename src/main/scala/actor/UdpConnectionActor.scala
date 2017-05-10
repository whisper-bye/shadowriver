package im.xun.shadowriver.tcpip

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

import akka.util.ByteString
import im.xun.shadowriver.Utils._
import im.xun.shadowriver._
import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import scodec.Attempt.Successful
import scodec.Codec
import scodec.bits.{BitVector, ByteVector}
import ColorString._

//把app传来的udp链接通过tunnel发送给代理服务器
class UdpConnectionActor(tuple: TcpConnectionTuple,
                         vpnActor: ActorRef,
                         server: InetSocketAddress,
                         val vpnService: ShadowRiverVpnService) extends Actor with ActorLogging
  with DnsRequest with DnsResponse with DnsHttpResolver
{


  val target = new InetSocketAddress(tuple.dst.toString, tuple.dstPort.value)
  val tunActor = context.actorOf(Props(classOf[TunActor],target,server,tuple.protocol,vpnService),"tunActor")
//  val tunActor = context.actorOf(Props(classOf[TunLocalActor],target,server,tuple.protocol,vpnService),"tunActor")

  val id = Math.abs(scala.util.Random.nextInt()%65535)
  lazy val respIpHeader = {
    IpHeader(8, id, 64, PROTOCOL_UDP, tuple.dst, tuple.src)
  }
  lazy val respUdpHeader = {
    UdpHeader(tuple.dstPort, tuple.srcPort, 0, 0)
  }

  def dnsResolver(name: String): IPV4 = {
    val addr = resolver(name)
    log.info(s"DNS: dnsResolver $addr")
    val ips = addr.split('.')
    def s2b(s: String) = {
      Integer.valueOf(s)
    }
    val ip = ips.map(s2b)
    IPV4(ip(0),ip(1),ip(2),ip(3))
  }

  def udpPkg(payload: ByteBuffer) = {
    val iph = respIpHeader.copy(dataLength = 8 + payload.limit())
    UdpPackage(iph, respUdpHeader,ByteVector.view(payload))
  }

  def receive = {
    case pkg: UdpPackage =>
      if(pkg.payload.length != 0) {
        log.info(s"UdpConnectionActor: data from App ${pkg.udpHeader.length} bytes!".magenta)
        tunActor ! TunWrite(pkg.payload.toByteBuffer,null)

      } else {
        log.error(s"UdpConnectionActor:  empty pkg!".magenta)
        log.error(pkg.udpHeader.toString)
        //        dumpBytes(pkg
      }

    case TunRead(data) =>
//      log.info(s"UdpConnectionActor: data from tun ${data.length} bytes!".magenta)
      val payload = data
      //udp header == 8
      val iph = respIpHeader.copy(dataLength = 8 + data.limit)
      val respPkg = UdpPackage(iph, respUdpHeader,ByteVector.view(payload))
      vpnActor ! UdpWrite(respPkg)

    case msg =>
      log.error("UdpConnectionActor: Unknown Message $msg".magenta)
  }

}
