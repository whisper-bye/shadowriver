package im.xun.shadowriver

import java.nio.ByteBuffer
import scala.language.implicitConversions

import scodec.Codec
import scodec.bits.{ByteVector, BitVector}

package object tcpip {

  /**
    * Computes the 16-bit one's complement checksum of the specified bit vector.
    *
    * @see [[https://tools.ietf.org/html/rfc1071]]
    */
  def checksum(bits: BitVector): BitVector = {
    //FIXME 16 bit aligned
    var sum = bits.bytes.grouped(2).foldLeft(0) { (acc, b) =>
      acc + b.toInt(signed = false)
    }
    while ((sum >> 16) != 0) {
      sum = (0xffff & sum) + (sum >> 16)
    }
    ~BitVector.fromInt(sum).drop(16)
  }

  def checksum(bytes: ByteVector): BitVector = {
    //FIXME 16 bit aligned
    var sum = bytes.grouped(2).foldLeft(0) { (acc, b) =>
      acc + b.toInt(signed = false)
    }
    while ((sum >> 16) != 0) {
      sum = (0xffff & sum) + (sum >> 16)
    }
    ~BitVector.fromInt(sum).drop(16)
  }

  def checksum(buf: Array[Byte]): Long = {
    var length = buf.length
    var i = 0
    var sum = 0L
    var data: Long = 0l
    while (length > 1) {
      data = ((buf(i) << 8) & 0xFF00) | (buf(i + 1) & 0xFF)
      sum += data
      if ((sum & 0xFFFF0000) > 0) {
        sum = sum & 0xFFFF
        sum += 1
      }
      i += 2
      length -= 2
    }
    if (length > 0) {
      sum += (buf(i) << 8 & 0xFF00)
      if ((sum & 0xFFFF0000) > 0) {
        sum = sum & 0xFFFF
        sum += 1
      }
    }
    sum = ~sum
    sum = sum & 0xFFFF
    sum
  }

  val PROTOCOL_UDP= 17
  val PROTOCOL_TCP= 6

  implicit def ipHeaderToByteBuffer(iph: IpHeader): ByteBuffer = {
    Codec.encode[IpHeader](iph).require.toByteBuffer
  }

  implicit def tcpHeaderToByteBuffer(tcph: TcpHeader): ByteBuffer = {
    Codec.encode[TcpHeader](tcph).require.toByteBuffer
  }

  implicit def tcpPackageToByteBuffer(pkg: TcpPackage): ByteBuffer = {
    Codec.encode[TcpPackage](pkg).require.toByteBuffer
  }

  implicit def udpHeaderToByteBuffer(udph: UdpHeader): ByteBuffer = {
    Codec.encode[UdpHeader](udph).require.toByteBuffer
  }

  implicit def udpPackageToByteBuffer(pkg: UdpPackage): ByteBuffer = {
    Codec.encode[UdpPackage](pkg).require.toByteBuffer
  }


}
