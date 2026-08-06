package sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import sk.styk.martin.apkanalyzer.core.apps.model.Permission
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PermissionDescriptionProvider @Inject constructor(@ApplicationContext private val context: Context) {

    fun describe(permission: Permission): String? = curatedDescriptions[permission.name]?.let(context::getString)
        ?: permission.details?.description?.takeIf { it.isNotBlank() }
        ?: permission.details?.declaringPackage
            ?.takeIf { it != PLATFORM_PACKAGE }
            ?.let { context.getString(R.string.permissions_defined_by, it) }

    private companion object {
        const val PLATFORM_PACKAGE = "android"

        val curatedDescriptions = mapOf(
            "android.permission.CAMERA" to R.string.permission_description_camera,
            "android.permission.RECORD_AUDIO" to R.string.permission_description_record_audio,
            "android.permission.ACCESS_FINE_LOCATION" to R.string.permission_description_location_fine,
            "android.permission.ACCESS_COARSE_LOCATION" to R.string.permission_description_location_coarse,
            "android.permission.ACCESS_BACKGROUND_LOCATION" to R.string.permission_description_location_background,
            "android.permission.READ_CONTACTS" to R.string.permission_description_contacts_read,
            "android.permission.WRITE_CONTACTS" to R.string.permission_description_contacts_write,
            "android.permission.GET_ACCOUNTS" to R.string.permission_description_accounts,
            "android.permission.READ_CALENDAR" to R.string.permission_description_calendar_read,
            "android.permission.WRITE_CALENDAR" to R.string.permission_description_calendar_write,
            "android.permission.READ_SMS" to R.string.permission_description_sms_read,
            "android.permission.SEND_SMS" to R.string.permission_description_sms_send,
            "android.permission.RECEIVE_SMS" to R.string.permission_description_sms_receive,
            "android.permission.READ_CALL_LOG" to R.string.permission_description_call_log_read,
            "android.permission.WRITE_CALL_LOG" to R.string.permission_description_call_log_write,
            "android.permission.CALL_PHONE" to R.string.permission_description_call_phone,
            "android.permission.READ_PHONE_STATE" to R.string.permission_description_phone_state,
            "android.permission.READ_PHONE_NUMBERS" to R.string.permission_description_phone_numbers,
            "android.permission.READ_EXTERNAL_STORAGE" to R.string.permission_description_storage_read,
            "android.permission.WRITE_EXTERNAL_STORAGE" to R.string.permission_description_storage_write,
            "android.permission.MANAGE_EXTERNAL_STORAGE" to R.string.permission_description_storage_manage,
            "android.permission.READ_MEDIA_IMAGES" to R.string.permission_description_media_images,
            "android.permission.READ_MEDIA_VIDEO" to R.string.permission_description_media_video,
            "android.permission.READ_MEDIA_AUDIO" to R.string.permission_description_media_audio,
            "android.permission.BODY_SENSORS" to R.string.permission_description_body_sensors,
            "android.permission.ACTIVITY_RECOGNITION" to R.string.permission_description_activity_recognition,
            "android.permission.BLUETOOTH_SCAN" to R.string.permission_description_bluetooth_scan,
            "android.permission.BLUETOOTH_CONNECT" to R.string.permission_description_bluetooth_connect,
            "android.permission.POST_NOTIFICATIONS" to R.string.permission_description_notifications,
            "android.permission.INTERNET" to R.string.permission_description_internet,
            "android.permission.ACCESS_NETWORK_STATE" to R.string.permission_description_network_state,
            "android.permission.ACCESS_WIFI_STATE" to R.string.permission_description_wifi_state,
            "android.permission.NFC" to R.string.permission_description_nfc,
            "android.permission.VIBRATE" to R.string.permission_description_vibrate,
            "android.permission.WAKE_LOCK" to R.string.permission_description_wake_lock,
            "android.permission.RECEIVE_BOOT_COMPLETED" to R.string.permission_description_boot_completed,
            "android.permission.FOREGROUND_SERVICE" to R.string.permission_description_foreground_service,
            "android.permission.REQUEST_INSTALL_PACKAGES" to R.string.permission_description_install_packages,
            "android.permission.SYSTEM_ALERT_WINDOW" to R.string.permission_description_overlay,
            "android.permission.SCHEDULE_EXACT_ALARM" to R.string.permission_description_exact_alarm,
            "android.permission.USE_EXACT_ALARM" to R.string.permission_description_exact_alarm,
            "android.permission.USE_BIOMETRIC" to R.string.permission_description_biometric,
            "android.permission.QUERY_ALL_PACKAGES" to R.string.permission_description_query_all_packages,
        )
    }
}
