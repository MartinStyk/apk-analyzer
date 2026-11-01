package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.apps

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

sealed interface AppsStepEvent {
    data object NavigateBack : AppsStepEvent
    data object NavigateToColumns : AppsStepEvent
    data object NavigateToFilter : AppsStepEvent
    data class NavigateToAppDetail(val packageName: PackageName) : AppsStepEvent
}
