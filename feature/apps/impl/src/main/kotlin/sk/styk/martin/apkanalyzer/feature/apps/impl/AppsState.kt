package sk.styk.martin.apkanalyzer.feature.apps.impl

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource

@Immutable
sealed interface AppsState {

    @Immutable
    data object Loading : AppsState

    @Immutable
    data class Ready(
        val apps: ImmutableList<AppListItem>,
        val recentApps: ImmutableList<AppListItem> = persistentListOf(),
        val searchQuery: String = "",
        val sortType: SortType = SortType.Name,
        val sortAscending: Boolean = true,
        val totalAppCount: Int = apps.size,
    ) : AppsState
}

@Immutable
data class AppListItem(
    val packageName: String,
    val applicationName: String,
    val targetSdk: Int,
    val apkSize: AppSize,
    val source: AppSource,
    val versionName: String?,
    val installTime: Long,
)

enum class SortType {
    Name,
    Size,
    InstallDate,
    TargetSdk,
}

