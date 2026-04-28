package sk.styk.martin.apkanalyzer.feature.apps.impl.search

sealed interface AppSearchEvent {
    data class NavigateToAppDetail(val packageName: String) : AppSearchEvent
    data object NavigateToFilter : AppSearchEvent
}
