package sk.styk.martin.apkanalyzer.core.appfunctions.model

import androidx.appfunctions.AppFunctionSerializable

/** Version, SDK targeting, install source, permission, and signing summary for one installed app. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppDetailResult(
    /** The app's unique package name, for example "com.example.app". */
    val packageName: String,
    /** The app's display name as the user sees it, for example "Chrome". */
    val applicationName: String,
    /** The app's user-visible version name, or null if the app doesn't declare one. */
    val versionName: String?,
    /** The app's internal version number. Not shown to users; use versionName for that. */
    val versionCode: Long,
    /** The Android version label the app targets, for example "Android 14", or null if unknown. */
    val targetSdkLabel: String?,
    /** The oldest Android version label the app supports, for example "Android 8.0", or null if unknown. */
    val minSdkLabel: String?,
    /** Where the app was installed from, for example "Google Play" or "Sideloaded". */
    val installSourceLabel: String,
    /** True if this is a system app that came preinstalled on the device, rather than one the user installed. */
    val isSystemApp: Boolean,
    /** How many permissions the app declares in its manifest. */
    val requestedPermissionCount: Int,
    /** True if the app is signed with more than one certificate at the same time. */
    val hasMultipleSigners: Boolean,
)
