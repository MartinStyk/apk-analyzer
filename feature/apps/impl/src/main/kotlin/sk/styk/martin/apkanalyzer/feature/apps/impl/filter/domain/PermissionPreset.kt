package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
sealed class PermissionPreset(val permissions: Set<String>) {

    data object Sensitive : PermissionPreset(
        setOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_SMS",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.CALL_PHONE",
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_PHONE_NUMBERS",
            "android.permission.PROCESS_OUTGOING_CALLS",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO",
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
            "android.permission.USE_BIOMETRIC",
            "android.permission.USE_FINGERPRINT",
            "android.permission.BODY_SENSORS",
            "android.permission.BODY_SENSORS_BACKGROUND",
            "android.permission.ACTIVITY_RECOGNITION",
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_ADVERTISE",
        ),
    )

    data object Camera : PermissionPreset(
        setOf("android.permission.CAMERA"),
    )

    data object Microphone : PermissionPreset(
        setOf("android.permission.RECORD_AUDIO"),
    )

    data object Location : PermissionPreset(
        setOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
        ),
    )

    data object Contacts : PermissionPreset(
        setOf(
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
        ),
    )

    data object Sms : PermissionPreset(
        setOf(
            "android.permission.READ_SMS",
            "android.permission.SEND_SMS",
        ),
    )

    data object Storage : PermissionPreset(
        setOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
        ),
    )

    companion object {
        val all: ImmutableList<PermissionPreset> by lazy {
            persistentListOf(
                Sensitive,
                Camera,
                Microphone,
                Location,
                Contacts,
                Sms,
                Storage,
            )
        }
    }
}
