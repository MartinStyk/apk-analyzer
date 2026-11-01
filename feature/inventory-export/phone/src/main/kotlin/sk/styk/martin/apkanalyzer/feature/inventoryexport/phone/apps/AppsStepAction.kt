package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.apps

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

sealed interface AppsStepAction {
    data object NavigateBack : AppsStepAction
    data object Continue : AppsStepAction
    data object EditFilters : AppsStepAction
    data object IncludeAllApps : AppsStepAction
    data class AppClicked(val packageName: PackageName) : AppsStepAction
}
