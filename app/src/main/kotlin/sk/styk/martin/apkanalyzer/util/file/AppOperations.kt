package sk.styk.martin.apkanalyzer.util.file

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import sk.styk.martin.apkanalyzer.R
import sk.styk.martin.apkanalyzer.core.common.logger.Logger

object AppOperations {
    fun openAppSystemPage(context: Context, packageName: String) {
        try {
            context.startActivity(
                Intent().apply {
                    action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = "package:$packageName".toUri()
                },
            )
        } catch (exception: Exception) {
            Logger.e("Actions", exception, "Could not open system page for $packageName.")
        }
    }

    @JvmStatic
    fun openGooglePlay(context: Context, packageName: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
        } catch (activityNotFound: ActivityNotFoundException) {
            // Google Play not installed, open it in browser
            Logger.w("Actions", activityNotFound, "Starting Google play failed. Try to open it in browser.")
            openBrowser(context, "https://play.google.com/store/apps/details?id=$packageName")
        }
    }

    @JvmStatic
    fun startForeignActivity(context: Context, packageName: String, activityName: String) {
        val intent =
            Intent().apply {
                component = ComponentName(packageName, activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Logger.e("Actions", e, "Starting foreign activity failed. Intent was $intent")
//            Toast.makeText(context, R.string.activity_run_failed, Toast.LENGTH_SHORT).show() TODO
        }
    }

    @JvmStatic
    fun openBrowser(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (activityNotFoundException: ActivityNotFoundException) {
            Logger.w("Actions", activityNotFoundException, "Can not open browser.")
//            Toast.makeText(context, R.string.activity_run_failed, Toast.LENGTH_SHORT).show() TODO
        }
    }
}
