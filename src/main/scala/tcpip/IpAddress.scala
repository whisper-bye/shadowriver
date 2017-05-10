package im.xun.shadowriver.tcpip

import scodec.bits.ByteVector
import scodec.Codec
import scodec.codecs

case class IpAddress(value: Int) {
  override def toString = ByteVector.fromInt(value).toIterable.map { b => 0xff & b.toInt }.mkString(".")
}

object IpAddress {
  implicit val codec: Codec[IpAddress] = codecs.int32.xmap[IpAddress](v => IpAddress(v), _.value)

  def fromString(str: String): Either[String, IpAddress] = {
    val V4Pattern = """^0*([0-9]{1,3})\.0*([0-9]{1,3})\.0*([0-9]{1,3})\.0*([0-9]{1,3})$""".r
    val result = str match {
      case V4Pattern(aa, bb, cc, dd) =>
        val (a, b, c, d) = (aa.toInt, bb.toInt, cc.toInt, dd.toInt)
        if (a >= 0 && a <= 255 && b >= 0 && b <= 255 && c >= 0 && c <= 255 && d >= 0 && d <= 255)
          Some(IpAddress((a << 24) | (b << 16) | (c << 8) | d))
        else None
      case other =>
        None
    }
    result.fold(Left(s"invalid IPv4 address: $str"): Either[String, IpAddress])(Right(_))
  }

  def fromStringValid(str: String): IpAddress =
    fromString(str).fold(err => throw new IllegalArgumentException(err), identity)
}
