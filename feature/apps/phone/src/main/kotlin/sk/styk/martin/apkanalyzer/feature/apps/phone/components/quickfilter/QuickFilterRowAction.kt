package sk.styk.martin.apkanalyzer.feature.apps.phone.components.quickfilter

import sk.styk.martin.apkanalyzer.core.apps.api.filtering.ActivityQuickFilter
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.QuickFilter
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.SourceQuickFilter
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.AppDataPermission

sealed interface QuickFilterRowAction {
    data class QuickFilterToggle(val filter: QuickFilter) : QuickFilterRowAction
    data class SourceQuickFilterToggled(val filter: SourceQuickFilter) : QuickFilterRowAction
    data class ActivityQuickFilterSelected(val filter: ActivityQuickFilter?) : QuickFilterRowAction
    data object FilterClick : QuickFilterRowAction
    data object DismissPermissionRationale : QuickFilterRowAction
    data class OpenPermissionSettings(val permission: AppDataPermission) : QuickFilterRowAction
}
