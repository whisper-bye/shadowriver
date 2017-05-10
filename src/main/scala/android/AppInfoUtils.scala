package im.xun.shadowriver

import im.xun.shadowriver.tcpip._


object AppInfoUtils {

  lazy val apps = AndroidUtils.getInstalledAppInfo

  val appMap = new java.util.concurrent.ConcurrentHashMap[TcpConnectionTuple, AppInfo]()

  def parseIpAddr(ipaddr: String) = {
    val arr = ipaddr.split(":")
    val ipr = java.lang.Long.parseLong(arr(0), 16).toInt
    val ip = Integer.reverseBytes(ipr)
    val port = java.lang.Long.parseLong(arr(1), 16).toInt
    (IpAddress(ip), Port(port))
  }

  def parseLine(line: String, protocol: Int): (TcpConnectionTuple,Int)= {
    val arr = line.trim.split(" +")
    val (src, srcPort) = parseIpAddr(arr(1))
    val (dst, dstPort) = parseIpAddr(arr(2))
    val uid = java.lang.Long.parseLong(arr(7), 10).toInt
    (TcpConnectionTuple(src,dst,srcPort,dstPort,protocol), uid)
  }

  def update() = {
    appMap.clear()
    updateTupleMap("/proc/net/tcp", PROTOCOL_TCP)
    updateTupleMap("/proc/net/udp", PROTOCOL_UDP)
  }

  def updateTupleMap(procFile:String,protocol: Int) = {
    val lines= scala.io.Source.fromFile(procFile).getLines()
    for(line <- lines) {
      try{
        val (tuple,uid) = parseLine(line,protocol)
        val app = apps.find(_.uid == uid)
        if(app.nonEmpty) {
//          log.info(s"AppInfoUtils: Find App Connection! ${app.get.appName}")
          appMap.put(tuple,app.get)
        } else {
//          log.info(s"AppInfoUtils: Connection not belong to App, uid=$uid")
        }
      } catch {
        case e: Exception =>
          log.info(s"AppInfoUtils: Fail to parse line $line $e")
      }
    }


  }

  def appForTuple(tuple: TcpConnectionTuple): Option[AppInfo] = {
    val appInfo = appMap.get(tuple)
    Option(appInfo)
  }

  def appForTupleUpdate(tuple: TcpConnectionTuple): Option[AppInfo] = {
    update()
    appForTuple(tuple)
  }

}
