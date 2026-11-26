package sk.styk.martin.apkanalyzer.feature.apps.phone.filter

import sk.styk.martin.apkanalyzer.core.apps.api.filtering.AppSizeRange
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.DateRange
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.PermissionPreset
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.UnusedAppsPeriod
import sk.styk.martin.apkanalyzer.core.common.model.AppSource

sealed interface FilterAction {
    data class SourceToggled(val source: AppSource, val selected: Boolean) : FilterAction
    data class SdkVersionToggled(val sdkVersion: Int) : FilterAction
    data class ApkSizeRangeChanged(val range: AppSizeRange) : FilterAction
    data class TotalSizeRangeChanged(val range: AppSizeRange) : FilterAction
    data class InstallTimeRangeChanged(val range: DateRange) : FilterAction
    data object InstallTimeRangeCleared : FilterAction
    data object Apply : FilterAction
    data object Reset : FilterAction
    data object NavigateBack : FilterAction
    data object SaveAndClose : FilterAction
    data object DiscardChanges : FilterAction
    data object DismissUnsavedChangesSheet : FilterAction
    data class UnusedPeriodSelected(val period: UnusedAppsPeriod) : FilterAction
    data object OpenUsagePermissionSettings : FilterAction
    data object OpenPermissionFilter : FilterAction
    data object OpenSourceFilter : FilterAction
    data object OpenSdkVersionFilter : FilterAction
    data class PermissionPresetToggled(val preset: PermissionPreset) : FilterAction
}
