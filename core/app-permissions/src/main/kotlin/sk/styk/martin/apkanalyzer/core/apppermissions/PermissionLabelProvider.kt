package sk.styk.martin.apkanalyzer.core.apppermissions

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionLabelProvider @Inject constructor(@ApplicationContext private val context: Context, private val packageManager: PackageManager) {
    private val knownLabels: Map<String, String> by lazy { buildKnownLabels() }

    fun getLabel(permissionName: String): String = knownLabels[permissionName]
        ?: loadFromPackageManager(permissionName)
        ?: createSimpleName(permissionName)

    private fun loadFromPackageManager(permissionName: String): String? = try {
        val info = packageManager.getPermissionInfo(permissionName, 0)
        val label = info.loadLabel(packageManager).toString()
        if (label.isNotBlank() && label != permissionName) label else null
    } catch (_: Exception) {
        null
    }

    private fun createSimpleName(name: String): String {
        val lastDot = name.lastIndexOf(".")
        val base = if (lastDot > 0 && lastDot < name.length) name.substring(lastDot + 1) else name
        val builder = StringBuilder(base)
        var i = 0
        var previousSpace = false
        while (++i < builder.length) {
            if (builder[i] == '_') {
                builder.replace(i, i + 1, " ")
                previousSpace = true
            } else {
                builder.replace(i, i + 1, if (previousSpace) builder[i].toString() else builder[i].lowercaseChar().toString())
                previousSpace = false
            }
        }
        return builder.toString()
    }

    private fun buildKnownLabels(): Map<String, String> = mapOf(
        "android.permission.CAMERA" to context.getString(R.string.permission_label_camera),
        "android.permission.RECORD_AUDIO" to context.getString(R.string.permission_label_microphone),
        "android.permission.ACCESS_FINE_LOCATION" to context.getString(R.string.permission_label_location_precise),
        "android.permission.ACCESS_COARSE_LOCATION" to context.getString(R.string.permission_label_location_approximate),
        "android.permission.ACCESS_BACKGROUND_LOCATION" to context.getString(R.string.permission_label_location_background),
        "android.permission.READ_CONTACTS" to context.getString(R.string.permission_label_contacts_read),
        "android.permission.WRITE_CONTACTS" to context.getString(R.string.permission_label_contacts_write),
        "android.permission.GET_ACCOUNTS" to context.getString(R.string.permission_label_accounts),
        "android.permission.READ_SMS" to context.getString(R.string.permission_label_sms_read),
        "android.permission.SEND_SMS" to context.getString(R.string.permission_label_sms_send),
        "android.permission.RECEIVE_SMS" to context.getString(R.string.permission_label_sms_receive),
        "android.permission.RECEIVE_MMS" to context.getString(R.string.permission_label_mms_receive),
        "android.permission.RECEIVE_WAP_PUSH" to context.getString(R.string.permission_label_wap_push),
        "android.permission.READ_CALL_LOG" to context.getString(R.string.permission_label_call_log_read),
        "android.permission.WRITE_CALL_LOG" to context.getString(R.string.permission_label_call_log_write),
        "android.permission.CALL_PHONE" to context.getString(R.string.permission_label_call_phone),
        "android.permission.READ_PHONE_STATE" to context.getString(R.string.permission_label_phone_state),
        "android.permission.READ_PHONE_NUMBERS" to context.getString(R.string.permission_label_phone_numbers),
        "android.permission.PROCESS_OUTGOING_CALLS" to context.getString(R.string.permission_label_outgoing_calls),
        "android.permission.READ_EXTERNAL_STORAGE" to context.getString(R.string.permission_label_storage_read),
        "android.permission.WRITE_EXTERNAL_STORAGE" to context.getString(R.string.permission_label_storage_write),
        "android.permission.READ_MEDIA_IMAGES" to context.getString(R.string.permission_label_media_images),
        "android.permission.READ_MEDIA_VIDEO" to context.getString(R.string.permission_label_media_video),
        "android.permission.READ_MEDIA_AUDIO" to context.getString(R.string.permission_label_media_audio),
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to context.getString(R.string.permission_label_media_selected),
        "android.permission.USE_BIOMETRIC" to context.getString(R.string.permission_label_biometric),
        "android.permission.USE_FINGERPRINT" to context.getString(R.string.permission_label_fingerprint),
        "android.permission.BODY_SENSORS" to context.getString(R.string.permission_label_body_sensors),
        "android.permission.BODY_SENSORS_BACKGROUND" to context.getString(R.string.permission_label_body_sensors_background),
        "android.permission.ACTIVITY_RECOGNITION" to context.getString(R.string.permission_label_activity_recognition),
        "android.permission.BLUETOOTH_SCAN" to context.getString(R.string.permission_label_bluetooth_scan),
        "android.permission.BLUETOOTH_CONNECT" to context.getString(R.string.permission_label_bluetooth_connect),
        "android.permission.BLUETOOTH_ADVERTISE" to context.getString(R.string.permission_label_bluetooth_advertise),
        "android.permission.BLUETOOTH" to context.getString(R.string.permission_label_bluetooth),
        "android.permission.UWB_RANGING" to context.getString(R.string.permission_label_uwb),
        "android.permission.POST_NOTIFICATIONS" to context.getString(R.string.permission_label_notifications),
        "android.permission.INTERNET" to context.getString(R.string.permission_label_internet),
        "android.permission.NFC" to context.getString(R.string.permission_label_nfc),
        "android.permission.VIBRATE" to context.getString(R.string.permission_label_vibrate),
        "android.permission.FLASHLIGHT" to context.getString(R.string.permission_label_flashlight),
        "android.permission.CHANGE_WIFI_STATE" to context.getString(R.string.permission_label_wifi_change),
        "android.permission.ACCESS_WIFI_STATE" to context.getString(R.string.permission_label_wifi_state),
        "android.permission.ACCESS_NETWORK_STATE" to context.getString(R.string.permission_label_network_state),
        "android.permission.WAKE_LOCK" to context.getString(R.string.permission_label_wake_lock),
        "android.permission.RECEIVE_BOOT_COMPLETED" to context.getString(R.string.permission_label_boot),
        "android.permission.FOREGROUND_SERVICE" to context.getString(R.string.permission_label_foreground_service),
        "android.permission.REQUEST_INSTALL_PACKAGES" to context.getString(R.string.permission_label_install_packages),
        "android.permission.SYSTEM_ALERT_WINDOW" to context.getString(R.string.permission_label_overlay),
        "android.permission.MANAGE_EXTERNAL_STORAGE" to context.getString(R.string.permission_label_manage_storage),
        "android.permission.SCHEDULE_EXACT_ALARM" to context.getString(R.string.permission_label_schedule_alarm),
        "android.permission.USE_EXACT_ALARM" to context.getString(R.string.permission_label_schedule_alarm),
    )

    companion object {
        // kept empty — labels moved to string resources
    }
}
