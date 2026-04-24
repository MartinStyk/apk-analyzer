package sk.styk.martin.apkanalyzer.feature.apps.impl.search

sealed interface AppSearchAction {
    data class QueryChanged(val query: String) : AppSearchAction
    data class AppClicked(val packageName: String) : AppSearchAction
    data class HistoryQueryClicked(val query: String) : AppSearchAction
    data class DeleteHistoryItem(val query: String) : AppSearchAction
    data object ClearHistory : AppSearchAction
}
