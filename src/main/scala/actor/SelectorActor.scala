package im.xun.shadowriver

import java.nio.channels.SelectionKey._
import scala.concurrent.duration._
import java.nio.channels.{SelectionKey, CancelledKeyException, SelectableChannel, Selector}
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor._

import scala.concurrent.ExecutionContext

case class SelectorRegister(channel: SelectableChannel, initialOps: Int)

case object ChannelConnectable
case object ChannelAcceptable
case object ChannelReadable extends DeadLetterSuppression
case object ChannelWritable extends DeadLetterSuppression

trait ChannelRegistration extends NoSerializationVerificationNeeded {
  def enableInterest(op: Int)
  def disableInterest(op: Int)
}

object SelectorHandler {
  // always set the interest keys on the selector thread,
  // benchmarks show that not doing so results in lock contention
  def enableInterestOps(key: SelectionKey, ops: Int): Unit = {
  val currentOps = key.interestOps
  val newOps = currentOps | ops
  if (newOps != currentOps) key.interestOps(newOps)
  }

  def disableInterestOps(key: SelectionKey, ops: Int): Unit = {
    val currentOps = key.interestOps
    val newOps = currentOps & ~ops
    if (newOps != currentOps) key.interestOps(newOps)
  }

}

class SelectorActor extends Actor with ActorLogging {
  log.info(s"Selector Actor started! ${self.path}")
  private[this] val wakeUp = new AtomicBoolean(false)
  val selector = Selector.open

//  val executorService = Executors.newFixedThreadPool(1)
//  val executionContext = ExecutionContext.fromExecutorService(executorService)

  def selectNow = {
    val sel = selector.selectNow()
    if (sel > 0) {
      // This assumes select return value == selectedKeys.size
      val keys = selector.selectedKeys
      val iterator = keys.iterator()
      val OP_READ_WRITE = OP_READ | OP_WRITE
      while (iterator.hasNext) {
        val key = iterator.next()
        if (key.isValid) {
          try {
            // Cache because the performance implications of calling this on different platforms are not clear
            val readyOps = key.readyOps()
            log.info(s"ReadOps: $readyOps")
            key.interestOps(key.interestOps & ~readyOps) // prevent immediate reselection by always clearing
            val connection = key.attachment.asInstanceOf[ActorRef]
            readyOps match {
              case OP_READ ⇒ connection ! ChannelReadable
              case OP_WRITE ⇒ connection ! ChannelWritable
              case OP_READ_WRITE => {
                connection ! ChannelWritable
                connection ! ChannelReadable
              }
              case x if (x & OP_ACCEPT) > 0 ⇒ connection ! ChannelAcceptable
              case x if (x & OP_CONNECT) > 0 ⇒
                SelectorHandler.disableInterestOps(key, OP_CONNECT)
                SelectorHandler.enableInterestOps(key,OP_READ|OP_WRITE)
                connection ! ChannelConnectable
              case x ⇒ log.warning("Invalid readyOps: [{}]", x)
            }
          } catch {
            case _: CancelledKeyException ⇒
            // can be ignored because this exception is triggered when the key becomes invalid
            // because `channel.close()` in `TcpConnection.postStop` is called from another thread
          }
        }
      }
      keys.clear() // we need to remove the selected keys from the set, otherwise they remain selected
    }
  }

  wakeUp.set(false)
  import context.dispatcher
  context.system.scheduler.scheduleOnce(100.micro,self,"tick")

  def receive = {
    case SelectorRegister(channel, initialOps) =>
      log.info(s"Receive register req: $channel")
      val channelActor = sender()
      log.info("A wake me!")
      if (wakeUp.compareAndSet(false, true)) // if possible avoid syscall and trade off with LOCK CMPXCHG
        selector.wakeup()

      val key = channel.register(selector,SelectionKey.OP_CONNECT, channelActor)

    case "tick" =>
      selectNow
      context.system.scheduler.scheduleOnce(100.micro,self,"tick")


//      channelActor ! new ChannelRegistration {
//        def enableInterest(ops: Int): Unit = enableInterestOps(key, ops)
//        def disableInterest(ops: Int): Unit = disableInterestOps(key, ops)
//      }


    case msg =>
      log.error(s"Unexpected message! $msg")
  }
}
