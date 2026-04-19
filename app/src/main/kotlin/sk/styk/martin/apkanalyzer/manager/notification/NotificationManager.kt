package sk.styk.martin.apkanalyzer.manager.notification

import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.IntRange
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import sk.styk.martin.apkanalyzer.R
import sk.styk.martin.apkanalyzer.core.common.resources.ResourcesManager
import javax.inject.Inject
import javax.inject.Singleton
import android.app.NotificationManager as AndroidNotificationManager

private const val ICON_EXPORT_CHANNEL_ID = "Icon export channel"
private const val ICON_EXPORT_NOTIFICATION_ID = 1_01

private const val APP_EXPORT_CHANNEL_ID = "App export channel"
private const val APP_EXPORT_NOTIFICATION_ID = 1_02

private const val MANIFEST_EXPORT_CHANNEL_ID = "Manifest export channel"
private const val MANIFEST_EXPORT_NOTIFICATION_ID = 1_03

@Singleton
class NotificationManager
@Inject
constructor(@ApplicationContext private val context: Context, private val resourcesManager: ResourcesManager, private val androidNotificationManager: AndroidNotificationManager) {
    private val flagImmutable = PendingIntent.FLAG_IMMUTABLE
    private val shouldShowNotification: Boolean
        get() = androidNotificationManager.areNotificationsEnabled()

    fun showImageExportedNotification(appName: String, drawableFileUri: Uri) {
//        if (!shouldShowNotification) {
//            return
//        }
//
//        createChannel(ICON_EXPORT_CHANNEL_ID, resourcesManager.getString(R.string.icon_save_channel))
//
//        val intent = Intent().apply {
//            action = Intent.ACTION_VIEW
//            setDataAndType(drawableFileUri, "image/png")
//            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        }
//
//        val stackBuilder = TaskStackBuilder.create(context).apply {
//            addParentStack(MainActivity::class.java)
//            addNextIntent(intent)
//        }
//        val openIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable)
//
//        val notification = notificationBuilder(ICON_EXPORT_CHANNEL_ID)
//            .setContentTitle(resourcesManager.getString(R.string.app_icon_saved, appName))
//            .setSmallIcon(R.drawable.ic_apkanalyzer_notification)
//            .setCategory(NotificationCompat.CATEGORY_REMINDER)
//            .setContentIntent(openIntent)
//            .addAction(R.drawable.ic_image, resourcesManager.getString(R.string.action_show), openIntent)
//            .build()
//
//        androidNotificationManager.notify(ICON_EXPORT_NOTIFICATION_ID, notification)
    }

    fun showAppExportProgressNotification(appName: String): NotificationCompat.Builder? {
        if (!shouldShowNotification) {
            return null
        }

        val notification =
            notificationBuilder(APP_EXPORT_CHANNEL_ID)
//            .setContentTitle(resourcesManager.getString(R.string.saving_app, appName))
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
//        if (!shouldShowNotification) {
//            return
//        }
//
//        cancelNotification(APP_EXPORT_NOTIFICATION_ID)
//        createChannel(APP_EXPORT_CHANNEL_ID, resourcesManager.getString(R.string.app_save_channel))
//
//        val intent = Intent().apply {
//            action = Intent.ACTION_GET_CONTENT
//            setDataAndType(outputFileUri, "resource/folder")
//            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        }
//        val stackBuilder = TaskStackBuilder.create(context).apply {
//            addParentStack(MainActivity::class.java)
//            addNextIntent(intent)
//        }
//        val openIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable)
//
//        val notification = notificationBuilder(APP_EXPORT_CHANNEL_ID)
//            .setContentTitle(resourcesManager.getString(R.string.saved_app, appName))
//            .setSmallIcon(R.drawable.ic_apkanalyzer_notification)
//            .setOngoing(false)
//            .addAction(R.drawable.ic_get_app, resourcesManager.getString(R.string.action_show), openIntent)
//            .setContentIntent(openIntent)
//            .build()
//
//        androidNotificationManager.notify(APP_EXPORT_NOTIFICATION_ID, notification)
    }

    fun showManifestSavedNotification(appName: String, outputFileUri: Uri) {
//        if (!shouldShowNotification) {
//            return
//        }
//
//        createChannel(MANIFEST_EXPORT_CHANNEL_ID, resourcesManager.getString(R.string.manifest_save_channel))
//
//        val openIntent = TaskStackBuilder.create(context).apply {
//            addParentStack(MainActivity::class.java)
//            addNextIntent(
//                Intent().apply {
//                    action = Intent.ACTION_VIEW
//                    setDataAndType(outputFileUri, "text/xml")
//                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                },
//            )
//        }.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable)
//
//        val notification = notificationBuilder(MANIFEST_EXPORT_CHANNEL_ID)
//            .setContentTitle(resourcesManager.getString(R.string.save_manifest_background_notification_title_done, appName))
//            .setSmallIcon(R.drawable.ic_apkanalyzer_notification)
//            .addAction(R.drawable.ic_file, resourcesManager.getString(R.string.action_show), openIntent)
//            .setContentIntent(openIntent)
//            .build()
//
//        androidNotificationManager.notify(MANIFEST_EXPORT_NOTIFICATION_ID, notification)
    }

    private fun notificationBuilder(channelId: String) = NotificationCompat.Builder(context, channelId)

    private fun createChannel(channelId: String, channelName: CharSequence) {
        val channel = NotificationChannel(channelId, channelName, AndroidNotificationManager.IMPORTANCE_DEFAULT)
        androidNotificationManager.createNotificationChannel(channel)
    }

    private fun cancelNotification(notificationId: Int) {
        androidNotificationManager.cancel(notificationId)
    }
}
