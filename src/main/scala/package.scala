package im.xun

package object shadowriver {

  //val bufferPool = new DirectByteBufferPool(128*1024, 1000)
  val bufferPool = new DirectByteBufferPool(1500, 5000)

//  val log = org.slf4j.LoggerFactory.getLogger("ShadowRiver:")
}
