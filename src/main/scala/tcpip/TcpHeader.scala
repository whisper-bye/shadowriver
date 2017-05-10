package im.xun.shadowriver.tcpip

import im.xun.shadowriver.ColorString._
import scodec.bits.BitVector
import scodec.{SizeBound, Codec}
import scodec.codecs._
//import scodec.stream._
import shapeless.HNil

case class TcpFlags(cwr: Boolean = false,
                    ecn: Boolean = false,
                    urg: Boolean = false,
                    ack: Boolean = false,
                    psh: Boolean = false,
                    rst: Boolean = false,
                    syn: Boolean = false,
                    fin: Boolean = false) {
  def b2n(v: Boolean) = {
    if(v) "1".red else "0".green
  }
  override def toString = {
    s"cwr:${b2n(cwr)} ecn:${b2n(ecn)} urg:${b2n(urg)} ack:${b2n(ack)} psh:${b2n(psh)} rst:${b2n(rst)} syn:${b2n(syn)} fin:${b2n(fin)}"
  }
}

object TcpFlags {
  implicit val codec: Codec[TcpFlags] = {
      ("cwr" | bool(1)) ::
      ("ecn" | bool(1)) ::
      ("urg" | bool(1)) ::
      ("ack" | bool(1)) ::
      ("psh" | bool(1)) ::
      ("rst" | bool(1)) ::
      ("syn" | bool(1)) ::
      ("fin" | bool(1))
  }.as[TcpFlags]
}

case class TcpHeader(sourcePort: Port,
                     destinationPort: Port,
                     sequenceNumber: Long,
                     ackNumber: Long,
                     dataOffset: Int,
                     flags: TcpFlags,
                     windowSize: Int,
                     checksum: Int,
                     urgentPointer: Int,
                     options: Vector[Long])  {
  override def toString = {
    val opts = options.map("%X".format(_)).mkString("")
    s"srcPort:$sourcePort dstPort:$destinationPort seq:$sequenceNumber ack:$ackNumber $flags ws:$windowSize chksum:$checksum opts:$opts"
  }
}

//http://www.freesoft.org/CIE/Course/Section4/8.htm
object TcpHeader {
  val port = Codec[Port]
  implicit val codec: Codec[TcpHeader] = {
    val componentCodec = {
        ("source port" | port) ::
        ("destination port" | port) ::
        ("seqNumber" | uint32) ::
        ("ackNumber" | uint32) :: //96
        (
          ("dataOffset" | uint4) >>:~ { headerWords =>
            ("reserved" | ignore(4)) ::
              ("flags" | Codec[TcpFlags]) ::
              ("windowSize" | uint16) :: //128
              ("checksum" | uint16) ::
              ("urgentPointer" | uint16) ::
            ("options" | vectorOfN(provide(headerWords - 5 ), uint32))
          })
    }.dropUnits//.as[TcpHeader]

    new Codec[TcpHeader] {
      def sizeBound = SizeBound.unknown
//      def sizeBound = SizeBound.exact(160)

      def encode(header: TcpHeader) = {
        for {
          encoded <- componentCodec.encode(
            header.sourcePort
              :: header.destinationPort
              :: header.sequenceNumber
              :: header.ackNumber
              :: header.dataOffset
              :: header.flags
              :: header.windowSize
              :: header.checksum
              :: header.urgentPointer
              :: header.options
              :: HNil)
        } yield encoded
      }

      def decode(bits: BitVector) = {
        componentCodec.decode(bits) map { _ map {
          h =>
            val t = h.tupled
            TcpHeader(t._1, t._2,t._3, t._4, t._5,t._6,t._7, t._8, t._9, t._10)
        }}
      }
    }
  }

//    def sdecoder(protocol: Int): StreamDecoder[TcpHeader] =
//      if (protocol == Protocols.Tcp) decode.once[TcpHeader]
//      else decode.empty

}
