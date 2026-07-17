package sk.styk.martin.apkanalyzer.manager.notification

import android.content.Context
import android.net.Uri
import androidx.annotation.IntRange
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import sk.styk.martin.apkanalyzer.R
import javax.inject.Inject
import javax.inject.Singleton
import android.app.NotificationManager as AndroidNotificationManager

private const val APP_EXPORT_CHANNEL_ID = "App export channel"
private const val APP_EXPORT_NOTIFICATION_ID = 1_02

@Singleton
class NotificationManager
@Inject
constructor(@ApplicationContext private val context: Context, private val androidNotificationManager: AndroidNotificationManager) {
    private val shouldShowNotification: Boolean
        get() = androidNotificationManager.areNotificationsEnabled()

    fun showImageExportedNotification(appName: String, drawableFileUri: Uri) {
    }

    fun showAppExportProgressNotification(appName: String): NotificationCompat.Builder? {
        if (!shouldShowNotification) {
            return null
        }

        val notification =
            notificationBuilder(APP_EXPORT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_apkanalyzer_notification)
                .setOngoing(true)
                .setProgress(100, 0, false)

        androidNotificationManager.notify(APP_EXPORT_NOTIFICATION_ID, notification.build())
        return notification
    }

    fun updateAppExportProgressNotification(notificationBuilder: NotificationCompat.Builder, @IntRange(from = 0, to = 100) progress: Int): NotificationCompat.Builder {
        notificationBuilder.setProgress(100, progress, false)
        androidNotificationManager.notify(APP_EXPORT_NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    fun showAppExportDoneNotification(appName: String, outputFileUri: Uri) {
    }

    fun showManifestSavedNotification(appName: String, outputFileUri: Uri) {
    }

    private fun notificationBuilder(channelId: String) = NotificationCompat.Builder(context, channelId)
}
