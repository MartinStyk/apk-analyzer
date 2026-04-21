package sk.styk.martin.apkanalyzer.feature.apps.impl

sealed interface AppsEvent {
    data class NavigateToAppDetail(val packageName: String) : AppsEvent
}

