package sk.styk.martin.apkanalyzer.core.uilibrary.launcher

import android.content.ActivityNotFoundException
import androidx.activity.result.ActivityResultLauncher
import sk.styk.martin.apkanalyzer.core.common.logger.Logger

private const val TAG = "ActivityResultLauncher"

fun <I> ActivityResultLauncher<I>.launchSafely(input: I, onUnavailable: () -> Unit) {
    try {
        launch(input)
    } catch (e: ActivityNotFoundException) {
        Logger.w(TAG, e, "No activity available to handle launcher input")
        onUnavailable()
    }
}
