package sk.styk.martin.apkanalyzer.feature.appdetail.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class AppHistoryTimelineNavKey(val packageName: String, val appName: String) : NavKey
