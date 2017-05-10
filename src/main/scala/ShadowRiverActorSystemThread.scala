//package im.xun.shadowriver
//
//import java.io.FileDescriptor
//
//import akka.actor.{Props, ActorSystem}
//import com.typesafe.config.ConfigFactory
//
//class ShadowRiverActorSystemThread(netFd: FileDescriptor,vpnService: ShadowRiverVpnService) extends Thread {
//
//
//  val conf = ConfigFactory.load("application")
//  val system = ActorSystem("ShadowRiver", conf)
//  lazy val vpnActor = system.actorOf(Props(classOf[VpnActor],netFd, vpnService), "vpnActor")
//
//  def stopThread(): Unit = {
//    log.info("stop ShadowRiverActorSystemThread!")
//    system.shutdown()
//    //clean up
//  }
//
//  override def run() = {
//    log.info(s"ShadowRiverActorSystemThread: ${Thread.currentThread().getName()}")
//    log.info("start ShadowRiverActorSystemThread")
//    vpnActor
//  }
//}
