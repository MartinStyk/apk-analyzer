package sk.styk.martin.apkanalyzer.feature.apps.impl.filter

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterState
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppSizeRange

@Immutable
data class FilterState(
    val filter: AppFilterState = AppFilterState(),
    val sizeFullRange: AppSizeRange? = null,
    val availableSdkVersions: ImmutableList<Int> = persistentListOf(),
    val hasUnsavedChanges: Boolean = false,
    val showUnsavedChangesSheet: Boolean = false,
    val isUsagePermissionGranted: Boolean = false,
)
