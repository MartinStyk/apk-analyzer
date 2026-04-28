package sk.styk.martin.apkanalyzer.feature.apps.impl.list

sealed interface AppsEvent {
    data class NavigateToAppDetail(val packageName: String) : AppsEvent
    data object NavigateToSearch : AppsEvent
    data object NavigateToSettings : AppsEvent
    data object NavigateToShowApkDetails : AppsEvent
    data object NavigateToFilter : AppsEvent
}
