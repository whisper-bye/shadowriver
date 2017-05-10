package im.xun.shadowriver

import android.content.pm.{ApplicationInfo, PackageManager}
import android.graphics.drawable.Drawable
import android.os.AsyncTask

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import android.widget.TextView
import scala.collection.JavaConversions._

import scala.util.Try

case class AppInfo(appName: String, pkgName: String, uid: Int, icon: Drawable)

object AndroidUtils {

  implicit val exec = ExecutionContext.fromExecutor(
    AsyncTask.THREAD_POOL_EXECUTOR)

  implicit def textViewToString(tv: TextView): String = {
    tv.getText.toString
  }

  lazy val pm = App.context.getApplicationContext.getPackageManager

  def getIcon(pkgName: String): Drawable = {
    Try{
      pm.getApplicationIcon(pkgName)
    } getOrElse App.context.getDrawable(R.drawable.abc_btn_check_material)
  }

  def getDataDir(pkgName: String) = {
    pm.getApplicationInfo(pkgName,0).dataDir
  }

  def getInstalledAppInfo: Seq[AppInfo] = {
    for{
      pkg <- pm.getInstalledApplications(PackageManager.GET_META_DATA)
//      if (pkg.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0
      appName = try {pm.getApplicationLabel(pkg).toString} catch {case e => "UnNamed App"}
    } yield AppInfo(appName, pkg.packageName, pkg.uid, getIcon(pkg.packageName))
  }

}
