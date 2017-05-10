//package im.xun.shadowriver.tcpip
//
//import java.net.{InetSocketAddress, SocketAddress}
//import java.nio.ByteBuffer
//import java.nio.channels.SocketChannel
//
//import im.xun.shadowriver.Main._
//import im.xun.shadowriver.Utils._
//import im.xun.shadowriver._
//import scodec.Attempt.Successful
//import scodec.Codec
//import scodec.bits.{ByteVector, BitVector}
//import ColorString._
//
//object TcpState {
//  val LISTENING = 10
//  val SYN_RCV = 0
//  val ESTABLISHED = 1
//  val CLOSE_WAIT = 3
//  val LAST_ACK = 4
//  val CLOSE = 5
//}
//
//class State {
//  var state = TcpState.LISTENING
//  var seqNum = scala.util.Random.nextLong
//}
///**
//  * Created by wuhx on 6/15/16.
//  */
//trait TcpMachine { self: NetFd =>
//  val state = new State
//
//
//
//  def respIpHeader(iph: IpHeader) = {
//    IpHeader(20, iph.id, iph.ttl, PROTOCOL_TCP, iph.destinationIp, iph.sourceIp)
//  }
//
//  def respTcpHeader(tcph: TcpHeader) = {
//    TcpHeader(sourcePort = tcph.destinationPort,
//      destinationPort = tcph.sourcePort,
//      sequenceNumber = state.seqNum,
//      ackNumber = tcph.sequenceNumber + 1,
//      dataOffset = 5,
//      flags = TcpFlags(fin = true, ack=true),
//      65535,
//      0,
//      0,
//      Vector.empty[Long]
//    )
//  }
//
//  def stateMachine(pkg: TcpPackage) = {
//
//  }
//
//  def finAck(pkg: TcpPackage) = {
//
//    val sip = pkg.ipHeader
//    val ip = IpHeader(20, sip.id, sip.ttl, PROTOCOL_TCP, sip.destinationIp, sip.sourceIp)
//
//    val tcp =
//      TcpHeader(sourcePort = pkg.tcpHeader.destinationPort,
//        destinationPort = pkg.tcpHeader.sourcePort,
//        sequenceNumber = state.seqNum,
//        ackNumber = pkg.tcpHeader.sequenceNumber + 1,
//        dataOffset = 5,
//        flags = TcpFlags(rst = true, ack=true),
//        65535,
//        0,
//        0,
//        Vector.empty[Long]
//      )
//
//    //    val combo: Array[ByteBuffer] = Array(ip,tcp)
//    out.write(TcpPackage(ip, tcp))
//
//  }
//
//  def syncAck(pkg: TcpPackage) = {
//
//    val sip = pkg.ipHeader
//    val ip = IpHeader(20, sip.id, sip.ttl, PROTOCOL_TCP, sip.destinationIp, sip.sourceIp)
//
//    val tcp =
//      TcpHeader(sourcePort = pkg.tcpHeader.destinationPort,
//        destinationPort = pkg.tcpHeader.sourcePort,
//        sequenceNumber = state.seqNum,
//        ackNumber = pkg.tcpHeader.sequenceNumber + 1,
//        dataOffset = 5,
//        flags = TcpFlags(ack = true, syn = true),
//        65535,
//        0,
//        0,
//        Vector.empty[Long]
//      )
//
//    state.state = TcpState.SYN_RCV
//    state.seqNum = state.seqNum + 1
//    //    val combo: Array[ByteBuffer] = Array(ip,tcp)
//    out.write(TcpPackage(ip, tcp))
//
//  }
//
//
//  def tcpProxy(pkg: TcpPackage) = {
//    val sip = pkg.ipHeader
//    val ip = IpHeader(20, sip.id, sip.ttl, PROTOCOL_TCP, sip.destinationIp, sip.sourceIp)
//
//    log.info(s"IP : ${pkg.ipHeader}".green)
//    log.info(s"TCP: ${pkg.tcpHeader}".green)
//    log.info(s"received:  ${pkg.payloadLen}")
//    val tcp =
//      TcpHeader(sourcePort = pkg.tcpHeader.destinationPort,
//        destinationPort = pkg.tcpHeader.sourcePort,
//        sequenceNumber = state.seqNum,
//        ackNumber = pkg.tcpHeader.sequenceNumber + pkg.payloadLen,
//        dataOffset = 5,
//        flags = TcpFlags(ack = true),
//        65535,
//        0,
//        0,
//        Vector.empty[Long]
//      )
//
//
//    //    val combo: Array[ByteBuffer] = Array(ip,tcp)
//    out.write(TcpPackage(ip, tcp))
//
//    val socketChannel = SocketChannel.open()
//    val remote = new InetSocketAddress(pkg.ipHeader.destinationIp.toString, pkg.tcpHeader.destinationPort.value)
////    val remote =  new InetSocketAddress("ifconfig.co",80)
//    log.info(s"Connect to $remote")
//    socketChannel.connect(remote)
//    val buf = pkg.payload.toByteBuffer
//    dumpBytes(buf)
//    while(buf.hasRemaining) {
//      val count = socketChannel.write(buf)
//      log.info(s"write $count bytes: total ${pkg.payloadLen}")
//    }
//
//    val bufIn = ByteBuffer.allocate(1024)
//    val bytesRead = socketChannel.read(bufIn)
//    bufIn.flip()
//    log.info(s"read $bytesRead bytes")
//    dumpBytes(bufIn)
//
//
////    val payload =
////      """HTTP/1.1 200 OK
////        |Server: nginx
////        |Date: Tue, 14 Jun 2016 09:44:54 GMT
////        |Content-Type: text/plain; charset=utf-8
////        |Content-Length: 15
////        |Connection: keep-alive
////        |Strict-Transport-Security: max-age=31536000; includeSubdomains; preload
////        |
////        |180.110.210.63
////        |
////      """.stripMargin
//
//    val dataPkg = TcpPackage(ip.copy(dataLength = ip.dataLength + bufIn.limit()),
//      tcp.copy(flags = TcpFlags(ack=true, psh=true)),
////      ByteVector.view(payload.getBytes)
//        ByteVector.view(bufIn)
//    )
//    out.write(dataPkg)
//    state.seqNum = state.seqNum + dataPkg.payloadLen
//
//  }
//
//  def handle(pkg: TcpPackage) = {
//    val flags = pkg.tcpHeader.flags
//    if (flags.syn) {
//      syncAck(pkg)
//    } else if (flags.fin) {
//      log.info("close connection!")
//      finAck(pkg)
//    } else if (flags.ack && pkg.payloadLen == 0) {
//      log.info("ignore ack!")
//    }
//    else {
//      tcpProxy(pkg)
//    }
//  }
//
//  def processRead(data: ByteBuffer) = {
//    val bits = BitVector.view(data)
//
//    Codec.decode[TcpPackage](bits) match {
//      case Successful(result) =>
//        val pkg = result.value
//        handle(pkg)
//      case e =>
//        log.error(s"fail to decode tcp pkg $e")
//        dumpBytes(data,data.limit())
//    }
//
//  }
//}
