package sk.styk.martin.apkanalyzer.feature.apps.tv.filter

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.ActivityQuickFilter
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.PermissionPreset
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.QuickFilter
import sk.styk.martin.apkanalyzer.core.common.model.AppSource

@Immutable
internal data class TvAppFilterState(
    val matchingAppCount: Int = 0,
    val isActive: Boolean = false,
    val sources: List<TvCountedOption<AppSource>> = listOf(),
    val installQuickFilters: List<TvFilterOption<QuickFilter>> = listOf(),
    val usageQuickFilters: List<TvFilterOption<QuickFilter>> = listOf(),
    val activityFilters: List<TvFilterOption<ActivityQuickFilter>> = listOf(),
    val permissionPresets: List<TvFilterOption<PermissionPreset>> = listOf(),
    val sdkVersions: List<TvSdkOption> = listOf(),
)

@Immutable
internal data class TvFilterOption<T>(val value: T, val selected: Boolean)

@Immutable
internal data class TvCountedOption<T>(
    val value: T,
    val selected: Boolean,
    val appCount: Int,
)

@Immutable
internal data class TvSdkOption(
    val sdk: Int,
    val versionName: String?,
    val selected: Boolean,
    val appCount: Int,
)
