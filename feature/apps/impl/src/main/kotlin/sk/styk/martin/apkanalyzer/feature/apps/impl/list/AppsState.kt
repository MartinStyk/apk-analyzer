package sk.styk.martin.apkanalyzer.feature.apps.impl.list

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import sk.styk.martin.apkanalyzer.core.common.model.AppDataPermission
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterState
import java.time.Instant

@Immutable
data class AppsState(
    val apps: AppListState = AppListState.Loading,
    val recents: RecentsState = RecentsState.Loading,
    val sortType: SortType = SortType.Name,
    val sortAscending: Boolean = true,
    val activeFilter: AppFilterState = AppFilterState(),
    val sortPermissionRationale: AppDataPermission? = null,
)

sealed interface AppListState {
    data object Loading : AppListState

    @Immutable
    data class Content(val apps: ImmutableList<AppListItem>) : AppListState
}

sealed interface RecentsState {
    data object Loading : RecentsState
    data object NoRecents : RecentsState

    @Immutable
    data class Content(val apps: ImmutableList<AppListItem>) : RecentsState
}

@Immutable
data class AppListItem(
    val packageName: PackageName,
    val applicationName: String,
    val targetSdk: Int,
    val apkSize: AppSize,
    val totalSize: AppSize?,
    val lastUsedTime: Instant?,
    val installTime: Instant,
    val lastUpdateTime: Instant,
    val source: AppSource,
)

enum class SortType {
    Name,
    TotalSize,
    ApkSize,
    InstallDate,
    LastUpdated,
    LastUsed,
    TargetSdk,
}
