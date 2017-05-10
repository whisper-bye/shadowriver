package im.xun.shadowriver

import java.net.{SocketException, InetSocketAddress}
import java.nio.channels.{SelectionKey, SelectableChannel, DatagramChannel, SocketChannel}
import ColorString._
import im.xun.shadowriver.tcpip.Protocols

import scala.concurrent.duration._
import akka.actor._
import akka.util.{Timeout, ByteString}

import java.nio.ByteBuffer


class TunLocalTcpNioActor(target: InetSocketAddress, server: InetSocketAddress, vpnService: ShadowRiverVpnService)
  extends Actor with ActorLogging  with Stash{

  log.info(s"TunLocalTcpNioActor: connect to target $target")

  val socketChannel =  SocketChannel.open()
  socketChannel.configureBlocking(false)
  vpnService.protect(socketChannel.socket())

  socketChannel.connect(target)

//  val readActor = context.actorOf(Props(classOf[TunTcpReadActor], socketChannel,target, context.parent), "readActor")
  //TODO
  //akka://ShadowRiver/user/selectorActor
  val selectorActor = context.actorSelection("/user/selectorActor")
  selectorActor ! SelectorRegister(socketChannel, SelectionKey.OP_CONNECT)

  def configChannel(socketChannel: SocketChannel) = {
    try socketChannel.socket.setTcpNoDelay(true) catch {
      case e: SocketException ⇒
        //有些客户端不支持 setTcpNoDelay
        log.debug("Could not enable TcpNoDelay: {}", e.getMessage)
    }
  }

  def doRead() = {
    val buf = bufferPool.acquire()
    buf.clear()
    val res = socketChannel.read(buf)
    if(res != 0) {
      buf.flip()
      if(buf.limit > 0) {
        log.info(s"TunNioReadActor: Read ${buf.limit()} bytes!".red)
        context.parent ! TunRead(buf)
      }
    }
    buf
  }

  var channelWriteable = false
  def receive: Receive = {
    case ChannelReadable =>
      log.info("Channel Readable!")
      var buf = doRead()
      while(buf.limit == buf.capacity()) {
        log.info(s"TunNioReadActor: Remain data!".red)
        //TODO: Whatif data length is exactly buf.capacity
        buf = doRead()
      }

    case ChannelWritable =>
      log.info("Channel Writable!")
      channelWriteable = true
//      context.become(receive)
      unstashAll()

    case ChannelConnectable =>
      log.info("Channel Connectable!")
      if(socketChannel.finishConnect()) {
        log.info("Channel Connected!")
        configChannel(socketChannel)
      } else {
        log.error("Channel Fail to be Connected!")
      }

    case ChannelAcceptable =>
      log.info("Channel Acceptable!")

    case d @ TunWrite(data,buf) =>
      log.info(s"TcpTunLocalActor: TunWrite ${data.limit} bytes".blue)
      if(channelWriteable) {
        val res = socketChannel.write(data)
        assert(res == data.limit, s"Fail write all $res != ${data.limit()}")
        bufferPool.release(buf)
      } else {
        log.error("Channel not Writable yet! Stash Message!")
        stash()
      }

    case msg =>
      log.error(s"TcpTunLocalActor: Unexpected message $msg".blue)
  }
}

