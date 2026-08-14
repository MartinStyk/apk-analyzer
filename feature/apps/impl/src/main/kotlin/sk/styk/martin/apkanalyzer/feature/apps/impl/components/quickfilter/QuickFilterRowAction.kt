package sk.styk.martin.apkanalyzer.feature.apps.impl.components.quickfilter

import sk.styk.martin.apkanalyzer.feature.apps.impl.components.AppDataPermission
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.ActivityQuickFilter
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.QuickFilter
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.SourceQuickFilter

sealed interface QuickFilterRowAction {
    data class QuickFilterToggle(val filter: QuickFilter) : QuickFilterRowAction
    data class SourceQuickFilterToggled(val filter: SourceQuickFilter) : QuickFilterRowAction
    data class ActivityQuickFilterSelected(val filter: ActivityQuickFilter?) : QuickFilterRowAction
    data object FilterClick : QuickFilterRowAction
    data object DismissPermissionRationale : QuickFilterRowAction
    data class OpenPermissionSettings(val permission: AppDataPermission) : QuickFilterRowAction
}
