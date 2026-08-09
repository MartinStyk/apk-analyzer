package sk.styk.martin.apkanalyzer.core.common.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

private const val TAG = "AppOperations"

fun Context.openAppSystemPage(packageName: PackageName) {
    try {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
            },
        )
    } catch (e: ActivityNotFoundException) {
        Logger.e(TAG, e, "Could not open system page for $packageName")
    } catch (e: SecurityException) {
        Logger.e(TAG, e, "Could not open system page for $packageName")
    }
}

fun Context.openGooglePlay(packageName: PackageName) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
    } catch (e: ActivityNotFoundException) {
        Logger.w(TAG, e, "Play Store not installed, falling back to browser")
        openBrowser("https://play.google.com/store/apps/details?id=$packageName")
    }
}

fun Context.startForeignActivity(packageName: PackageName, activityName: String): Result<Unit> = runCatching {
    startActivity(
        Intent().apply {
            component = ComponentName(packageName.value, activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
    )
}.onFailure { Logger.e(TAG, it, "Could not start activity $activityName in $packageName") }

fun Context.sendForeignBroadcast(packageName: PackageName, receiverName: String): Result<Unit> = runCatching {
    sendBroadcast(Intent().apply { component = ComponentName(packageName.value, receiverName) })
}.onFailure { Logger.e(TAG, it, "Could not send broadcast to $receiverName in $packageName") }

fun Context.openBrowser(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: ActivityNotFoundException) {
        Logger.w(TAG, e, "No browser available to open $url")
    }
}
