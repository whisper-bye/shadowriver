package im.xun.shadowriver.tcpip

import java.net.InetSocketAddress

import akka.util.ByteString
import im.xun.shadowriver._
import im.xun.shadowriver.Utils._
import akka.actor._
import scodec.bits.ByteVector
import ColorString._


case class TcpConnectionTuple(src: IpAddress, dst: IpAddress, srcPort: Port, dstPort: Port, protocol: Int)

class TcpConnectionActor(tuple: TcpConnectionTuple,
                         vpnActor: ActorRef,
                         server: InetSocketAddress,
                         vpnService: ShadowRiverVpnService )
  extends Actor with ActorLogging with Stash{

  var ISN = Math.abs(scala.util.Random.nextInt)
  val id = Math.abs(scala.util.Random.nextInt()%65535)
  var cntPkgFromApp = 0
  var cntPkgToApp = 0
  var lastAckNum= 0L
  var peerISN = 0L
  var nextSeqNum = ISN
  val ackExpected = collection.mutable.Set[Long]()
  val tunPkgQueue = collection.mutable.Queue[TcpPackage]()

  val target = new InetSocketAddress(tuple.dst.toString, tuple.dstPort.value)
//  val tunActor = context.actorOf(Props(classOf[TunLocalActor],target,server,tuple.protocol,vpnService),"tunActor")
  val tunActor = context.actorOf(Props(classOf[TunActor],target,server,tuple.protocol,vpnService),"tunActor")
  log.info(s"TcpConnectionActor: Create TunActor:${tunActor}")

  def respIpHeader(dataLen: Int = 20) = {
    //FIXME
    IpHeader(dataLen, id, 64, PROTOCOL_TCP, tuple.dst, tuple.src)
  }

  def respTcpHeader(flags: TcpFlags,seqNum: Long, ackNum: Long, dataOffset: Int = 5, options:Vector[Long] =Vector.empty[Long]) = {
    TcpHeader(
      sourcePort      = tuple.dstPort,
      destinationPort = tuple.srcPort,
      sequenceNumber  = seqNum,
      ackNumber       = ackNum,
      dataOffset      = dataOffset, //5 * 4 = 20 tcp header size
      flags           = flags,
      65535,
      0,
      0,
      options
    )
  }

  def waitTunConnected: Receive = {
    case TunConnected =>
      log.info("TcpConnectionActor: TunConnected!")
      context.become(preSync)
      unstashAll()
    case msg =>
//      log.info(s"TcpConnectionActor: Tun not Connected stash msg $msg!")
      stash()
  }

  def preSync: Receive = {
    case TcpRead(pkg,buf) =>
      cntPkgFromApp = cntPkgFromApp + 1
      val flags = pkg.tcpHeader.flags
      if(flags == TcpFlags(syn = true)){
        log.info("TcpConnectionActor: Connection sync".magenta)
        peerISN = pkg.tcpHeader.sequenceNumber
        val flags = TcpFlags(syn=true, ack=true)
        lastAckNum = pkg.tcpHeader.sequenceNumber + 1
        val respPkg = TcpPackage(respIpHeader(24), respTcpHeader(flags, nextSeqNum,lastAckNum, 6,  Vector(0x20405B4L)))
        sender()  ! TcpWrite(respPkg,null)
        cntPkgToApp = cntPkgToApp + 1
        nextSeqNum = nextSeqNum + 1
        context.become(syncReceived)

      } else if(flags.fin) {
      } else {
          log.error("TcpConnectionActor: connection  should start with sync pkg!".magenta)
          log.error(pkg.tcpHeader.toString)
        val flags = TcpFlags(rst = true)
        val respPkg = TcpPackage(respIpHeader(), respTcpHeader(flags, nextSeqNum,lastAckNum))
        cntPkgToApp = cntPkgToApp + 1
        vpnActor ! TcpWrite(respPkg,null)
        context.stop(self)
//          dumpBytes(pkg)
      }
      if(buf!= null) {
        bufferPool.release(buf)
      }

    case msg =>
      log.error(s"TcpConnectionActor: Unknown Message: $msg".magenta)
  }

  def syncReceived: Receive = {
    case TcpRead(pkg,buf) =>
//    case pkg: TcpPackage =>
      cntPkgFromApp = cntPkgFromApp + 1
      val flags = pkg.tcpHeader.flags
      if(flags == TcpFlags(ack = true)){
        log.info("TcpConnectionActor: Connection ESTABLISHED".magenta)
        context.become(connectionEstablished)
      } else {
        log.error("syncReceived: Not expected pkg!")
        log.error(pkg.tcpHeader.toString)
      }
      if(buf!= null) {
        bufferPool.release(buf)
      }

    case msg =>
      log.error("Unknown Message: "+msg)
  }

//  def writeTunQueuedPkg() = {
//    if(tunPkgQueue.nonEmpty) {
//      val pkg = tunPkgQueue.dequeue()
//      vpnActor ! TcpWrite(pkg,null)
//      cntPkgToApp = cntPkgToApp + 1
//
//      ackExpected += nextSeqNum - ISN
//
//      log.info(s"Write To App: $cntPkgToApp seq:${pkg.tcpHeader.sequenceNumber - ISN} ack:${pkg.tcpHeader.ackNumber - peerISN} data:${pkg.payload.length} remain pkg in queue:${tunPkgQueue.size}")
//    } else {
//      log.info("TcpConnectionActor: tunPkgQueue empty!")
//    }
//
//  }

  def connectionEstablished: Receive = {
    case TcpRead(pkg,buf) =>
      cntPkgFromApp = cntPkgFromApp + 1
      val flags = pkg.tcpHeader.flags
      if(flags.syn) {
        log.error("We need restart tcp connection")
      }

      if(flags.fin || flags.rst) {
        log.info(s"TcpConnectionActor: Connection Terminator by App${pkg.tcpHeader.toString}".magenta)
        val flags = TcpFlags(rst = true)
        val respPkg = TcpPackage(respIpHeader(), respTcpHeader(flags, nextSeqNum,lastAckNum))
        cntPkgToApp = cntPkgToApp + 1
        vpnActor ! TcpWrite(respPkg,null)
        context.stop(self)
      } else{

        if(pkg.payloadLen != 0){
          log.info(s"TcpConnectionActor: data from App! ${pkg.payloadLen} bytes!".magenta)
          //        dumpBytes(pkg.payload.toByteBuffer)

          val flags = TcpFlags(ack=true)
          lastAckNum = pkg.tcpHeader.sequenceNumber + pkg.payloadLen
          val respPkg = TcpPackage(respIpHeader(), respTcpHeader(flags,nextSeqNum, lastAckNum))
          cntPkgToApp = cntPkgToApp + 1
          vpnActor ! TcpWrite(respPkg,null)
          tunActor ! TunWrite(pkg.payload.toByteBuffer, buf)
        }
//        if(flags.ack) {
//          val num = pkg.tcpHeader.ackNumber - ISN
//          if(ackExpected.contains(num)) {
//            for(n <- ackExpected) {
//              if(n <= num) {
//                ackExpected.remove(n)
//              }
//            }
//            log.info(s"TcpConnectionActor: SEQ-TRACE handle $num ack after $ackExpected ")
//            if(ackExpected.isEmpty) {
//              log.info(s"TcpConnectionActor: SEQ-TRACE send queue size:${tunPkgQueue.size}")
////              writeTunQueuedPkg()
//            }
//          } else {
//            log.error(s"TcpConnectionActor: SEQ-TRACE unexpected ack $num remain:$ackExpected ".red)
//          }
//        }
      }

    case TunRead(data) =>
      log.info(s"TcpConnectionActor: data from tun ${data.limit()} bytes!".magenta)
      val payload = data
      val flags = TcpFlags(ack=true, psh = true)
//      val flags = TcpFlags(ack=true)
      val iph = respIpHeader().copy(dataLength =respIpHeader().dataLength+ payload.limit())
      val respPkg = TcpPackage(iph, respTcpHeader(flags,nextSeqNum, lastAckNum),ByteVector.view(payload))
      nextSeqNum = nextSeqNum + data.limit
//      tunPkgQueue.enqueue(respPkg)
//      writeTunQueuedPkg()
//      if(ackExpected.isEmpty) {
//        //writeTunQueuedPkg()
//      }else {
//        log.info(s"TcpConnectionActor: SEQ-TRACE TunRead(data) wait for ack $ackExpected ")
//      }
      vpnActor ! TcpWrite(respPkg,data)

      log.info(s"TcpConnectionActor: : Total ${nextSeqNum - ISN} bytes from tun " )

    case msg =>
      log.error(s"TcpConnectionActor: Unknown Message $msg".magenta)
  }

  def receive = preSync
}
