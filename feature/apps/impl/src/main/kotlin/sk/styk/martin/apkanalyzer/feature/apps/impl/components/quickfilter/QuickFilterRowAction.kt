package sk.styk.martin.apkanalyzer.feature.apps.impl.components.quickfilter

import sk.styk.martin.apkanalyzer.core.common.model.AppDataPermission
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.QuickFilter

sealed interface QuickFilterRowAction {
    data class QuickFilterToggle(val filter: QuickFilter) : QuickFilterRowAction
    data object FilterClick : QuickFilterRowAction
    data object DismissPermissionRationale : QuickFilterRowAction
    data class OpenPermissionSettings(val permission: AppDataPermission) : QuickFilterRowAction
}
