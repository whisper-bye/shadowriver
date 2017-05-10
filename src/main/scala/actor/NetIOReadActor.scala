package im.xun.shadowriver

import java.io.{FileInputStream, FileDescriptor}
import java.nio.ByteBuffer

import akka.actor.{ActorRef, Actor}
import scodec.Attempt.Successful
import scodec.Codec
import scodec.bits.BitVector
import scala.concurrent.duration._
import Utils._
import tcpip._
import ColorString._

class NetIOReadActor(netFd: FileDescriptor, vpnActor: ActorRef) extends Actor {
  lazy val in = new FileInputStream(netFd).getChannel

//  val BUF_SIZE = 32767
//  val data = ByteBuffer.allocateDirect(BUF_SIZE)
  import context.dispatcher
  context.system.scheduler.scheduleOnce(1.micro, self, "tick")

//  //FIXME: block read will take up an akka dispatcher forever
//  while(true) {
//    Thread.sleep(100)
//    if(bufOpt.isEmpty){
//      bufOpt = Some(bufferPool.acquire())
//    }
//    val data = bufferPool.acquire()
//    data.clear()
//    var result = in.read(data)
//    while(result == 0) {
//      //should not be here
//      log.info("Read no data!!".red)
//      Thread.sleep(100)
//      result = in.read(data)
//    }
//    data.flip()
//
//
//  }

  //http://www.ibm.com/developerworks/java/tutorials/j-nio/j-nio.html
  //http://tutorials.jenkov.com/java-nio/selectors.html
//  context.system.scheduler.scheduleOnce(1.micro, self, "start")

  var bufOpt: Option[ByteBuffer] = None

  def decodePkg(data: ByteBuffer) = {
    val protocol: Int = data.get(9)
    if(protocol == PROTOCOL_TCP) {
      log.info(s"NetIORead: tcp pkg: ${data.limit()}".red)
      Codec.decode[TcpPackage](BitVector.view(data)) match {
        case Successful(decoded) =>
          val pkg = decoded.value
          vpnActor ! TcpRead(pkg,data)

        case e =>
          log.error(s"fail to decode tcp pkg $e")
          dumpBytes(data,data.limit())
      }
    } else {
      log.info(s"NetIORead: udp pkg: ${data.limit()}".red)
      Codec.decode[UdpPackage](BitVector(data)) match {
        case Successful(decoded) =>
          val pkg = decoded.value
          vpnActor ! UdpRead(pkg)

        case e =>
          log.error(s"fail to decode udp pkg $e")
          dumpBytes(data,data.limit())
      }
    }
  }

  def receive = {
    case "tick" =>
      if(bufOpt.isEmpty) {
        //bufOpt = Some(bufferPool.acquire())
        bufOpt = Some(bufferPool.acquire())
        bufOpt.get.clear()
      }

      val buf = bufOpt.get
      val res = in.read(buf)

      if(res != 0) {
        buf.flip()
        if(buf.limit > 0) {
          decodePkg(buf)
          bufOpt = None
        }
        context.system.scheduler.scheduleOnce(100.micro, self, "tick")
      }

      context.system.scheduler.scheduleOnce(100.micro, self, "tick")


    case msg =>
      log.error("Unexpected message: "+msg)

  }

}
