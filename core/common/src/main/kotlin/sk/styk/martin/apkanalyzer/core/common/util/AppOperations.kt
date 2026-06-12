package sk.styk.martin.apkanalyzer.core.common.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.net.toUri
import sk.styk.martin.apkanalyzer.core.common.logger.Logger

private const val TAG = "AppOperations"

fun Context.openAppSystemPage(packageName: String) {
    try {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
            },
        )
    } catch (e: Exception) {
        Logger.e(TAG, e, "Could not open system page for $packageName")
    }
}

fun Context.openGooglePlay(packageName: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
    } catch (e: ActivityNotFoundException) {
        Logger.w(TAG, e, "Play Store not installed, falling back to browser")
        openBrowser("https://play.google.com/store/apps/details?id=$packageName")
    }
}

fun Context.startForeignActivity(packageName: String, activityName: String) {
    val intent = Intent().apply {
        component = ComponentName(packageName, activityName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        startActivity(intent)
    } catch (e: Exception) {
        Logger.e(TAG, e, "Could not start activity $activityName in $packageName")
    }
}

fun Context.openBrowser(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Logger.w(TAG, e, "No browser available to open $url")
    }
}
