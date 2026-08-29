package sk.styk.martin.apkanalyzer.core.appfunctions.model

import androidx.appfunctions.AppFunctionSerializable
import java.time.Instant

/** A short identification of one installed app, returned by search and lookup functions. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppSummary(
    /** The app's unique package name, for example "com.example.app". Pass this to other functions that take a packageName. */
    val packageName: String,
    /** The app's display name as the user sees it, for example "Chrome". */
    val applicationName: String,
    /** The app's user-visible version name, or null if the app doesn't declare one. */
    val versionName: String?,
    /** Where the app was installed from, for example "Google Play" or "Sideloaded". */
    val installSourceLabel: String,
    /** The app's installed size in megabytes, including its data when that's known. */
    val sizeMb: Double,
    /** When the app was last opened, or null if that's never been recorded. */
    val lastUsedTime: Instant?,
)
