package sk.styk.martin.apkanalyzer.feature.appdetail.phone.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal data class AppHistoryChangeNavKey(
    val packageName: String,
    val snapshotId: Long,
    val appName: String,
) : NavKey
