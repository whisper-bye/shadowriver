package im.xun.shadowriver

import android.app.Activity
import android.app.Activity._
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import TypedResource._
import android.view.View
import android.view.View.OnClickListener

class MainActivity extends Activity with TypedFindView {
  log.info("Starting ShadowRiver!")
  implicit val context = this
  lazy val textview = findView(TR.text)
  lazy val btn = findView(TR.startVpn)

  /** Called when the activity is first created. */
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    textview.setText("Hello world, from " + TR.string.app_name.value)

    btn.setOnClickListener(new OnClickListener {
      override def onClick(v: View): Unit = {
        val intent = VpnService.prepare(context)
        if (intent != null) {
          startActivityForResult(intent, 0)
        } else {
          onActivityResult(0,RESULT_OK, null)
        }
      }

    })
  }

  override def onActivityResult(request: Int, result: Int, data: Intent) = {
    log.info("VPN UI: onActivityResult")
    if(result == RESULT_OK) {
      val intent = new Intent(this, classOf[ShadowRiverVpnService])
        .putExtra("ServerIp","192.168.1.11")
        .putExtra("ServerPort", "80")
        .putExtra("Secret","hellogfw")
      startService(intent)
    }
  }
}
