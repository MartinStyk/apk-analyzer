package sk.styk.martin.apkanalyzer.feature.apps.impl.search

import sk.styk.martin.apkanalyzer.feature.apps.impl.list.AppListItem

sealed interface AppSearchAction {
    data class QueryChanged(val query: String) : AppSearchAction
    data class AppClicked(val app: AppListItem) : AppSearchAction
    data class HistoryQueryClicked(val query: String) : AppSearchAction
    data class DeleteHistoryItem(val query: String) : AppSearchAction
    data object ClearHistory : AppSearchAction
    data object FilterClicked : AppSearchAction
    data object ClearAllFilters : AppSearchAction
}
