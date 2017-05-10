package im.xun.shadowriver.tcpip

import scodec.bits._
import scodec.{ Codec, SizeBound }
import scodec.codecs._
import scodec.codecs.literals._
//import scodec.stream._
import shapeless._

/** Simplified version of the IPv4 header format. */
case class IpHeader(
                         dataLength: Int,
                         id: Int,
                         ttl: Int,
                         protocol: Int,
                         sourceIp: IpAddress,
                         destinationIp: IpAddress
                       ) {
  override def toString = {
    s"dataLength:$dataLength src:$sourceIp dst:$destinationIp"
  }
}
//http://www.freesoft.org/CIE/Course/Section3/7.htm
object IpHeader {

  implicit val codec: Codec[IpHeader] = {
    val componentCodec = {
      // Word 1 --------------------------------
      ("version"         | bin"0100"     ) ::
        ("ihl"             | uint4         ) ::
        ("dscp"            | ignore(6)     ) ::
        ("ecn"             | ignore(2)     ) ::
        ("total_length"    | uint16        ) ::
        // Word 2 --------------------------------
        ("id"              | uint16        ) ::
        ("flags"           | ignore(3)     ) ::
        ("fragment_offset" | ignore(13)    ) ::
        // Word 3 --------------------------------
        ("ttl"             | uint8         ) ::
        ("proto"           | uint8         ) ::
        ("checksum"        | bits(16)      ) ::
        // Word 4 --------------------------------
        ("src_ip"          | Codec[IpAddress]) ::
        // Word 5 --------------------------------
        ("dest_ip"         | Codec[IpAddress])
    }.dropUnits

    new Codec[IpHeader] {
      def sizeBound = SizeBound.exact(160)

      def encode(header: IpHeader) = {
        val totalLength = header.dataLength + 20//ip header length
        for {
          encoded <- componentCodec.encode(
            5 :: totalLength
              :: header.id
              :: header.ttl
              :: header.protocol
              :: BitVector.low(16)
              :: header.sourceIp
              :: header.destinationIp
              :: HNil)
          chksum = checksum(encoded)
        } yield encoded.patch(80L, chksum)
      }

      def decode(bits: BitVector) = {
        componentCodec.decode(bits) map { _ map {
          h =>
            val t = h.tupled
            IpHeader(t._2 - 20, t._3, t._4, t._5, t._7, t._8)
        }}
      }
    }
  }

}
