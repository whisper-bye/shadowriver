package im.xun.shadowriver.tcpip

import java.nio.ByteBuffer

import scodec.bits._
import scodec.{ Codec, SizeBound }
import scodec.codecs._
import scodec.codecs.literals._
import shapeless._
import im.xun.shadowriver.Utils._
import im.xun.shadowriver._

/** Simplified version of the IPv4 header format. */
case class TcpPackage(ipHeader: IpHeader,
                      tcpHeader: TcpHeader,
                      payload: ByteVector = ByteVector.empty
                   ) {
  def payloadLen = ipHeader.dataLength  - tcpHeader.dataOffset * 4
}

object TcpPackage {

  def presudoHeaderBuild(pkg: TcpPackage) = {
    val tcpHeaderLen = pkg.tcpHeader.limit()
    val pseudoHeaderByteBuffer = ByteBuffer.allocate( 12 + tcpHeaderLen)
    pseudoHeaderByteBuffer.putInt(pkg.ipHeader.sourceIp.value)
    pseudoHeaderByteBuffer.putInt(pkg.ipHeader.destinationIp.value)
    pseudoHeaderByteBuffer.put(0x0.toByte)
    pseudoHeaderByteBuffer.put(PROTOCOL_TCP.toByte)
    pseudoHeaderByteBuffer.putShort((tcpHeaderLen+pkg.payloadLen).toShort)//FIXME
    pseudoHeaderByteBuffer.put(pkg.tcpHeader)
    pseudoHeaderByteBuffer.flip()
    pseudoHeaderByteBuffer
  }

  implicit val codec: Codec[TcpPackage] = {
    val componentCodec = {
        ("ip_header"         | Codec[IpHeader]    ) ::
        ("tcp_header"        | Codec[TcpHeader]   ) ::
        ("payload"           | bytes)
    }.dropUnits

    new Codec[TcpPackage] {
//      def sizeBound = SizeBound.exact(160)
      def sizeBound = SizeBound.unknown

      def encode(pkg: TcpPackage) = {
        for {
          encoded <- componentCodec.encode(
            pkg.ipHeader :: pkg.tcpHeader :: pkg.payload :: HNil
          )
          pseudoHeader = presudoHeaderBuild(pkg)
          payload = pkg.payload.toByteBuffer
          data = BitVector(pseudoHeader) ++ pkg.payload.toBitVector
          chksum = BitVector.fromLong(checksum(data.toByteArray),16)
        } yield encoded.patch(36L*8, chksum)
      }

      def decode(bits: BitVector) = {
        componentCodec.decode(bits) map { _ map {
          h =>
            val t = h.tupled
            TcpPackage(t._1, t._2, t._3)
        }}
      }
    }
  }

}
