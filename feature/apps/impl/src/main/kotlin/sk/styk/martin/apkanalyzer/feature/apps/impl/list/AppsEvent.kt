package sk.styk.martin.apkanalyzer.feature.apps.impl.list

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

sealed interface AppsEvent {
    data class NavigateToAppDetail(val packageName: PackageName) : AppsEvent
    data object NavigateToSearch : AppsEvent
    data object NavigateToSettings : AppsEvent
    data object NavigateToFilter : AppsEvent
    data object OpenUsageAccessSettings : AppsEvent
}
