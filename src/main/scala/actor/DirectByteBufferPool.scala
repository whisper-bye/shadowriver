package im.xun.shadowriver

import java.nio.ByteBuffer

trait BufferPool {
  def acquire(): ByteBuffer
  def release(buf: ByteBuffer)
}


class DirectByteBufferPool(defaultBufferSize: Int, maxPoolEntries: Int) extends BufferPool {
  private[this] val pool: Array[ByteBuffer] = new Array[ByteBuffer](maxPoolEntries)
  private[this] var buffersInPool: Int = 0

  def acquire(): ByteBuffer = {
    log.info(s"acquire buffer! buffers in pool ${buffersInPool}!")
    takeBufferFromPool()
  }


  def release(buf: ByteBuffer): Unit = {
    //TODO: debug only
    assert(buf.capacity() == defaultBufferSize, "Not acquire from me!")
    offerBufferToPool(buf)
    log.info(s"release buffer! buffers in pool ${buffersInPool}!")
  }

  private def allocate(size: Int): ByteBuffer =
    ByteBuffer.allocateDirect(size)

  private final def takeBufferFromPool(): ByteBuffer = {
    val buffer = pool.synchronized {
      if (buffersInPool > 0) {
        buffersInPool -= 1
        pool(buffersInPool)
      } else null
    }

    // allocate new and clear outside the lock
    if (buffer == null)
      allocate(defaultBufferSize)
    else {
      buffer.clear()
      buffer
    }
  }

  private final def offerBufferToPool(buf: ByteBuffer): Unit =
    pool.synchronized {
      if (buffersInPool < maxPoolEntries) {
        pool(buffersInPool) = buf
        buffersInPool += 1
      } // 如果pool没有满，引用该buf，防止其被GC清理
      else {
        log.info("buffer pool full!")
        // pool已满，不做任何处理，该buf将被垃圾回收。
      }
    }
}
