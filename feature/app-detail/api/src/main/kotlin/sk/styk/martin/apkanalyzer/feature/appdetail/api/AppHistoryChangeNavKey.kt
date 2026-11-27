package sk.styk.martin.apkanalyzer.feature.appdetail.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class AppHistoryChangeNavKey(
    val packageName: String,
    val snapshotId: Long,
    val appName: String,
) : NavKey
