package sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R

internal val PermissionItem.icon: ImageVector
    get() = permissionIcon(name = name, groupName = groupName)

internal fun permissionIcon(name: String, groupName: String?): ImageVector = permissionIcons[name] ?: groupIcons[groupName] ?: ApkAnalyzerIcons.Permissions

@get:StringRes
internal val PermissionScope.labelRes: Int
    get() = when (this) {
        PermissionScope.Requested -> R.string.permissions_scope_requested
        PermissionScope.Defined -> R.string.permissions_scope_defined
    }

@get:StringRes
internal val GrantState.labelRes: Int
    get() = when (this) {
        GrantState.Granted -> R.string.permissions_granted
        GrantState.NotGranted -> R.string.permissions_not_granted
    }

@get:StringRes
internal val PermissionItem.grantExplanationRes: Int?
    get() = when (val state = grantState) {
        null -> null

        else -> when (protectionLevel) {
            ProtectionLevel.Dangerous -> when (state) {
                GrantState.Granted -> R.string.permissions_grant_dangerous_granted
                GrantState.NotGranted -> R.string.permissions_grant_dangerous_not_granted
            }

            ProtectionLevel.Signature -> when (state) {
                GrantState.Granted -> when {
                    isSelfDeclared -> R.string.permissions_grant_signature_self
                    ProtectionFlag.Privileged in protectionFlags -> R.string.permissions_grant_signature_granted_privileged
                    else -> R.string.permissions_grant_signature_granted
                }

                GrantState.NotGranted -> R.string.permissions_grant_signature_not_granted
            }

            ProtectionLevel.Internal -> when (state) {
                GrantState.Granted -> R.string.permissions_grant_internal_granted
                GrantState.NotGranted -> R.string.permissions_grant_internal_not_granted
            }

            ProtectionLevel.Normal -> when (state) {
                GrantState.Granted -> R.string.permissions_grant_normal_granted
                GrantState.NotGranted -> R.string.permissions_grant_normal_not_granted
            }

            null -> R.string.permissions_grant_unavailable
        }
    }

@get:StringRes
internal val ProtectionLevel?.labelRes: Int
    get() = when (this) {
        ProtectionLevel.Dangerous -> R.string.permissions_level_dangerous
        ProtectionLevel.Signature -> R.string.permissions_level_signature
        ProtectionLevel.Internal -> R.string.permissions_level_internal
        ProtectionLevel.Normal -> R.string.permissions_level_normal
        null -> R.string.permissions_level_unavailable
    }

@get:StringRes
internal val ProtectionLevel?.explanationRes: Int
    get() = when (this) {
        ProtectionLevel.Dangerous -> R.string.permissions_level_dangerous_explanation
        ProtectionLevel.Signature -> R.string.permissions_level_signature_explanation
        ProtectionLevel.Internal -> R.string.permissions_level_internal_explanation
        ProtectionLevel.Normal -> R.string.permissions_level_normal_explanation
        null -> R.string.permissions_level_unavailable_explanation
    }

@get:StringRes
internal val ProtectionFlag.explanationRes: Int
    get() = when (this) {
        ProtectionFlag.Privileged -> R.string.permissions_flag_privileged
        ProtectionFlag.AppOp -> R.string.permissions_flag_appop
        ProtectionFlag.Instant -> R.string.permissions_flag_instant
        ProtectionFlag.Development -> R.string.permissions_flag_development
    }

private val permissionIcons = mapOf(
    "android.permission.CAMERA" to ApkAnalyzerIcons.Camera,
    "android.permission.RECORD_AUDIO" to ApkAnalyzerIcons.Microphone,
    "android.permission.ACCESS_FINE_LOCATION" to ApkAnalyzerIcons.Location,
    "android.permission.ACCESS_COARSE_LOCATION" to ApkAnalyzerIcons.Location,
    "android.permission.ACCESS_BACKGROUND_LOCATION" to ApkAnalyzerIcons.Location,
    "android.permission.READ_CONTACTS" to ApkAnalyzerIcons.Contacts,
    "android.permission.WRITE_CONTACTS" to ApkAnalyzerIcons.Contacts,
    "android.permission.GET_ACCOUNTS" to ApkAnalyzerIcons.Contacts,
    "android.permission.READ_CALENDAR" to ApkAnalyzerIcons.Calendar,
    "android.permission.WRITE_CALENDAR" to ApkAnalyzerIcons.Calendar,
    "android.permission.READ_SMS" to ApkAnalyzerIcons.Sms,
    "android.permission.SEND_SMS" to ApkAnalyzerIcons.Sms,
    "android.permission.RECEIVE_SMS" to ApkAnalyzerIcons.Sms,
    "android.permission.RECEIVE_MMS" to ApkAnalyzerIcons.Sms,
    "android.permission.READ_CALL_LOG" to ApkAnalyzerIcons.Phone,
    "android.permission.WRITE_CALL_LOG" to ApkAnalyzerIcons.Phone,
    "android.permission.CALL_PHONE" to ApkAnalyzerIcons.Phone,
    "android.permission.READ_PHONE_STATE" to ApkAnalyzerIcons.Phone,
    "android.permission.READ_PHONE_NUMBERS" to ApkAnalyzerIcons.Phone,
    "android.permission.READ_EXTERNAL_STORAGE" to ApkAnalyzerIcons.Folder,
    "android.permission.WRITE_EXTERNAL_STORAGE" to ApkAnalyzerIcons.Folder,
    "android.permission.MANAGE_EXTERNAL_STORAGE" to ApkAnalyzerIcons.Folder,
    "android.permission.READ_MEDIA_IMAGES" to ApkAnalyzerIcons.Folder,
    "android.permission.READ_MEDIA_VIDEO" to ApkAnalyzerIcons.Folder,
    "android.permission.READ_MEDIA_AUDIO" to ApkAnalyzerIcons.Folder,
    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to ApkAnalyzerIcons.Folder,
    "android.permission.BODY_SENSORS" to ApkAnalyzerIcons.Sensors,
    "android.permission.BODY_SENSORS_BACKGROUND" to ApkAnalyzerIcons.Sensors,
    "android.permission.ACTIVITY_RECOGNITION" to ApkAnalyzerIcons.ActivityRecognition,
    "android.permission.BLUETOOTH" to ApkAnalyzerIcons.Bluetooth,
    "android.permission.BLUETOOTH_SCAN" to ApkAnalyzerIcons.Bluetooth,
    "android.permission.BLUETOOTH_CONNECT" to ApkAnalyzerIcons.Bluetooth,
    "android.permission.BLUETOOTH_ADVERTISE" to ApkAnalyzerIcons.Bluetooth,
    "android.permission.POST_NOTIFICATIONS" to ApkAnalyzerIcons.Notifications,
    "android.permission.NEARBY_WIFI_DEVICES" to ApkAnalyzerIcons.Wifi,
    "android.permission.ACCESS_WIFI_STATE" to ApkAnalyzerIcons.Wifi,
    "android.permission.CHANGE_WIFI_STATE" to ApkAnalyzerIcons.Wifi,
)

private val groupIcons = mapOf(
    "android.permission-group.CAMERA" to ApkAnalyzerIcons.Camera,
    "android.permission-group.MICROPHONE" to ApkAnalyzerIcons.Microphone,
    "android.permission-group.LOCATION" to ApkAnalyzerIcons.Location,
    "android.permission-group.CONTACTS" to ApkAnalyzerIcons.Contacts,
    "android.permission-group.SMS" to ApkAnalyzerIcons.Sms,
    "android.permission-group.PHONE" to ApkAnalyzerIcons.Phone,
    "android.permission-group.CALL_LOG" to ApkAnalyzerIcons.Phone,
    "android.permission-group.STORAGE" to ApkAnalyzerIcons.Folder,
    "android.permission-group.READ_MEDIA_VISUAL" to ApkAnalyzerIcons.Folder,
    "android.permission-group.READ_MEDIA_AURAL" to ApkAnalyzerIcons.Folder,
    "android.permission-group.CALENDAR" to ApkAnalyzerIcons.Calendar,
    "android.permission-group.SENSORS" to ApkAnalyzerIcons.Sensors,
    "android.permission-group.NOTIFICATIONS" to ApkAnalyzerIcons.Notifications,
    "android.permission-group.NEARBY_DEVICES" to ApkAnalyzerIcons.Bluetooth,
    "android.permission-group.ACTIVITY_RECOGNITION" to ApkAnalyzerIcons.ActivityRecognition,
)
