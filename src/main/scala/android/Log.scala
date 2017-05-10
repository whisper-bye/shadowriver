package im.xun.shadowriver

object log {

  val TAG="ShadowRiver:"
  def debug(msg: String) = {
    android.util.Log.d(TAG, msg)
  }

  def info(msg: String) = {
    android.util.Log.i(TAG, msg)
  }

  def error(msg: String) = {
    android.util.Log.e(TAG, msg)
  }

}
