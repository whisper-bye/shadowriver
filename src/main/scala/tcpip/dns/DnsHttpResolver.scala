package im.xun.shadowriver


import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

import im.xun.shadowriver.tcpip.UdpConnectionActor


trait DnsHttpResolver { this: UdpConnectionActor =>

  val server = "119.29.29.29"

  def req(name: String) =
    s"""|GET /d?dn=$name HTTP/1.1
        |Host: 119.29.29.29
        |Connection: keep-alive
        |Pragma: no-cache
        |Cache-Control: no-cache
        |Upgrade-Insecure-Requests: 1
        |User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36
        |Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
        |Accept-Encoding: gzip, deflate, sdch
        |Accept-Language: en,zh-CN;q=0.8,zh;q=0.6,zh-TW;q=0.4
        |
    """.stripMargin.replace("\n","\r\n")


  val socket = SocketChannel.open()
  vpnService.protect(socket.socket())
  //protect socket
  socket.connect(new InetSocketAddress(server,80))
  socket.configureBlocking(false)

  def resolver(name: String): String = {
    val data = req(name).getBytes
    try {
      socket.write(ByteBuffer.wrap(data))
    } catch {
      case e: Throwable =>
        log.error(s"Fail to send DnsHttp req! $e")
    }
    Thread.sleep(100)

    val resp = ByteBuffer.allocate(1024)
    var res = socket.read(resp)
    while(res == 0){
      res = socket.read(resp)
      Thread.sleep(100)
    }
    resp.flip()
    val respString = new String(resp.array(),0,resp.limit())
    val ip = try {
      respString.split("\r\n").last.split(";").head
    } catch {
      case e: Exception =>
        log.error(s"HttpDns: Fail to parse ip! \n$respString \n $e")
        ""
    }
    ip
  }
}

