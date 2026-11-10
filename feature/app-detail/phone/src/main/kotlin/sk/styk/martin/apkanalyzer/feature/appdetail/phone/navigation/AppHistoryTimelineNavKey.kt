package sk.styk.martin.apkanalyzer.feature.appdetail.phone.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal data class AppHistoryTimelineNavKey(val packageName: String, val appName: String) : NavKey
