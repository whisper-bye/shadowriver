package im.xun.shadowriver

import android.app.Application
import android.content.Context

class App extends Application {
  import App._

  override def onCreate() {
    log.info("App init!")
    super.onCreate()
    context = getApplicationContext
  }
}

object App {
  var context: Context = _
}
