package sk.styk.martin.apkanalyzer.feature.apps.impl.filter

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterState
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppSizeRange

sealed interface ApkSizeSectionState {
    data object Loading : ApkSizeSectionState
    data class RangeAvailable(val bounds: AppSizeRange) : ApkSizeSectionState
}

sealed interface TotalSizeSectionState {
    data object PermissionMissing : TotalSizeSectionState
    data object Loading : TotalSizeSectionState
    data class RangeAvailable(val bounds: AppSizeRange) : TotalSizeSectionState
}

sealed interface UnusedAppsSectionState {
    data object PermissionMissing : UnusedAppsSectionState
    data object Loading : UnusedAppsSectionState
    data object Available : UnusedAppsSectionState
}

@Immutable
data class FilterState(
    val filter: AppFilterState = AppFilterState(),
    val apkSizeSectionState: ApkSizeSectionState = ApkSizeSectionState.Loading,
    val totalSizeSectionState: TotalSizeSectionState = TotalSizeSectionState.Loading,
    val unusedAppsSectionState: UnusedAppsSectionState = UnusedAppsSectionState.Loading,
    val availableSdkVersions: ImmutableList<Int> = persistentListOf(),
    val hasUnsavedChanges: Boolean = false,
    val showUnsavedChangesSheet: Boolean = false,
)
