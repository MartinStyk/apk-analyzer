package sk.styk.martin.apkanalyzer.feature.apps.impl.filter

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterState
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppSizeRange
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.PermissionPreset

@Immutable
sealed interface ApkSizeSectionState {
    data object Loading : ApkSizeSectionState
    data class RangeAvailable(val bounds: AppSizeRange) : ApkSizeSectionState
}

@Immutable
sealed interface TotalSizeSectionState {
    data object PermissionMissing : TotalSizeSectionState
    data object Loading : TotalSizeSectionState
    data class RangeAvailable(val bounds: AppSizeRange) : TotalSizeSectionState
}

@Immutable
sealed interface UnusedAppsSectionState {
    data object PermissionMissing : UnusedAppsSectionState
    data object Loading : UnusedAppsSectionState
    data object Available : UnusedAppsSectionState
}

@Immutable
data class SdkVersionEntry(val sdkVersion: Int, val androidVersionName: String?)

@Immutable
data class FilterState(
    val filter: AppFilterState = AppFilterState(),
    val apkSizeSectionState: ApkSizeSectionState = ApkSizeSectionState.Loading,
    val totalSizeSectionState: TotalSizeSectionState = TotalSizeSectionState.Loading,
    val unusedAppsSectionState: UnusedAppsSectionState = UnusedAppsSectionState.Loading,
    val availableSdkVersions: List<SdkVersionEntry> = listOf(),
    val availableSources: List<AppSource> = listOf(),
    val activePermissionPresets: List<PermissionPreset> = listOf(),
    val extraPermissionCount: Int = 0,
    val hasUnsavedChanges: Boolean = false,
    val showUnsavedChangesSheet: Boolean = false,
)
