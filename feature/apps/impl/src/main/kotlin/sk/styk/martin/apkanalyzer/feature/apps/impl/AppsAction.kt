package sk.styk.martin.apkanalyzer.feature.apps.impl

sealed interface AppsAction {
    data class SearchQueryChanged(val query: String) : AppsAction
    data class SortTypeSelected(val sortType: SortType) : AppsAction
    data class AppClicked(val packageName: String) : AppsAction
}

