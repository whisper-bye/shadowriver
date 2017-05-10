package im.xun.shadowriver

import ColorString._
import akka.actor.{Props, ActorSystem}
import android.content.Intent
import android.net.VpnService
import android.app.Service.START_STICKY
import android.os.IBinder
import com.typesafe.config.ConfigFactory

class ShadowRiverVpnService extends VpnService {
  log.info("ShadowRiverVpnService Starting!")

//  var vpnThread: ShadowRiverActorSystemThread = _

  lazy val interface = new Builder()
    .addAddress("10.0.0.1",24)
    .setMtu(1500)
    .addAllowedApplication("im.xun.android")
    .addAllowedApplication("acr.browser.lightning")
    .addAllowedApplication("com.tencent.mm")
    .addAllowedApplication("com.tencent.mm:push")
//      .addAllowedApplication("com.netease.newsreader.activity")
//    .addAllowedApplication("com.netease.newsreader.activity:ajmd")
//    .addAllowedApplication("com.netease.pomelo.news.push.messageservice_V1")
//    .addAllowedApplication("com.netease.newsreader.activity:pushservice")
//    .addAllowedApplication("com.netease.newsreader.activity:sync")
//    .addDnsServer("178.79.131.110")
    .addRoute("0.0.0.0",0)
    .establish()

  lazy val netFd = interface.getFileDescriptor

  var serverIp: Option[String] = None
  var serverPort: Option[String] = None
  var secret: Option[String] = None

  override def onBind(intent: Intent): IBinder = {
    log.info("ShadowRiverVpnService: onBind".red)
    null
  }

  override def onCreate() {
    log.info("ShadowRiverVpnService: onCreate".red)
    super.onCreate()
  }


  override def onRevoke(): Unit = {
    log.info("ShadowRiverVpnService OnRevoke".red)
  }

  lazy val conf = ConfigFactory.load("application")
  lazy val system = ActorSystem("ShadowRiver", conf)
  lazy val vpnActor = system.actorOf(Props(classOf[VpnActor],netFd, this), "vpnActor")
  lazy val selectorActor = system.actorOf(Props(classOf[SelectorActor]), "selectorActor")

  override def onStartCommand(intent: Intent, flags: Int, startId: Int) = {

    log.info(s"ShadowRiverVpnServiced: ${Thread.currentThread().getName}")
    vpnActor
    selectorActor
    //    log.info(s"ShadowRiverVpnService onStartCommand $serverIp")
    START_STICKY
  }


}

