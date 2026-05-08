package sk.styk.martin.apkanalyzer.feature.apps.impl.filter

import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppSizeRange
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.DateRange
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.UnusedAppsPeriod

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
}
