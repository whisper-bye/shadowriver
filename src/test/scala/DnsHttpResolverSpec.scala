package im.xun.shadowriver

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class DnsHttpResolverSpec
  extends FlatSpec
    with Matchers
    with DnsHttpResolver
{
  def xit = ignore

  it should "Resolve dns" in {
    val name = "m.baidu.com"
    val ip = resolver(name)
    log.info(s"ip resolved: $ip")

  }

}
