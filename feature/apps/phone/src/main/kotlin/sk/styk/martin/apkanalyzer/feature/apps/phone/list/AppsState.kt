package sk.styk.martin.apkanalyzer.feature.apps.phone.list

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.filtering.AppFilterState
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.sorting.AppSortType
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.AppDataPermission
import java.time.Instant

@Immutable
data class AppsState(
    val apps: AppListState = AppListState.Loading,
    val recents: RecentsState = RecentsState.Loading,
    val sortType: AppSortType = AppSortType.Name,
    val sortAscending: Boolean = true,
    val activeFilter: AppFilterState = AppFilterState(),
    val sortPermissionRationale: AppDataPermission? = null,
    val isInventoryExportEnabled: Boolean = false,
)

sealed interface AppListState {
    data object Loading : AppListState

    @Immutable
    data class Content(val apps: List<AppListItem>) : AppListState
}

sealed interface RecentsState {
    data object Loading : RecentsState
    data object NoRecents : RecentsState

    @Immutable
    data class Content(val apps: List<AppListItem>) : RecentsState
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

internal fun InstalledApp.toListItem() = AppListItem(
    packageName = packageName,
    applicationName = applicationName,
    targetSdk = targetSdk,
    apkSize = apkSize,
    totalSize = totalSize,
    installTime = installTime,
    lastUpdateTime = lastUpdateTime,
    lastUsedTime = lastUsedTime,
    source = source,
)
