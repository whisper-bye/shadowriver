package im.xun.shadowriver.tcpip

import scodec.Codec
import scodec.codecs._

case class UdpHeader(sourcePort: Port, destinationPort: Port, length: Int, checksum: Int)

object UdpHeader {
  implicit val codec: Codec[UdpHeader] = {
    val port = Codec[Port]
    ("source port"      | port) ::
      ("destination port" | port) ::
      ("length"           | uint16) ::
      ("checksum"         | uint16)
  }.as[UdpHeader]

}
