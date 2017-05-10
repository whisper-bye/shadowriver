package im.xun.shadowriver.tcpip

import scodec.bits._
import java.nio.ByteBuffer
import scodec.{ Codec, SizeBound }
import scodec.codecs._
import shapeless._

//http://www.erg.abdn.ac.uk/users/gorry/course/inet-pages/udp.html
//https://github.com/jop-devel/jop/blob/master/java/target/src/common/ejip/jtcpip/UDPPacket.java
//http://www4.ncsu.edu/~mlsichit/Teaching/407/Resources/udpChecksum.html
//http://www.faqs.org/rfcs/rfc768.html
case class UdpPackage(ipHeader: IpHeader,
                      udpHeader: UdpHeader,
                      payload: ByteVector = ByteVector.empty
                     )

object UdpPackage {

  def presudoHeaderBuild(pkg: UdpPackage) = {
    val udpHeaderLen = pkg.udpHeader.limit()
    val pseudoHeaderByteBuffer = ByteBuffer.allocate( 12 + udpHeaderLen)
    pseudoHeaderByteBuffer.putInt(pkg.ipHeader.sourceIp.value)
    pseudoHeaderByteBuffer.putInt(pkg.ipHeader.destinationIp.value)
    pseudoHeaderByteBuffer.put(0x0.toByte)
    pseudoHeaderByteBuffer.put(PROTOCOL_UDP.toByte)
    pseudoHeaderByteBuffer.putShort((8 + pkg.payload.length.intValue).toShort)//FIXME
    pseudoHeaderByteBuffer.flip()
    pseudoHeaderByteBuffer
  }

  val udpHeaderSize = 8
  implicit val codec: Codec[UdpPackage] = {
    val componentCodec = {
      ("ip_header"         | Codec[IpHeader]    ) ::
        ("udp_header"        | Codec[UdpHeader]   ) ::
        ("payload"           | bytes)
    }.dropUnits

    new Codec[UdpPackage] {
      def sizeBound = SizeBound.unknown

      def encode(pkg: UdpPackage) = {
        //设置udp头的长度字段
        val len: Int = 8 + pkg.payload.length.intValue
        val udpHeader = pkg.udpHeader.copy(length = len)
        for {
          encoded <- componentCodec.encode(
            pkg.ipHeader :: udpHeader :: pkg.payload :: HNil
          )
          pseudoHeader = presudoHeaderBuild(pkg)
          data = BitVector(pseudoHeader) ++ BitVector.view(udpHeader) ++ pkg.payload.toBitVector
          chksum = BitVector.fromLong(checksum(data.toByteArray),16)
//          chksum = checksum(data)//没2字节对齐
        } yield encoded.patch(26L*8, chksum)
      }

      def decode(bits: BitVector) = {
        componentCodec.decode(bits) map { _ map {
          h =>
            val t = h.tupled
            UdpPackage(t._1, t._2, t._3)
        }}
      }
    }
  }
}
