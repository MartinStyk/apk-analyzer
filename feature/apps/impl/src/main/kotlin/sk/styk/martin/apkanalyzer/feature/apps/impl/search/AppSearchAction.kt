package sk.styk.martin.apkanalyzer.feature.apps.impl.search

import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.apps.impl.list.AppListItem

sealed interface AppSearchAction {
    data class QueryChanged(val query: String) : AppSearchAction
    data class AppClicked(val app: AppListItem) : AppSearchAction
    data class HistoryItemClicked(val item: SearchHistoryItem) : AppSearchAction
    data class DeleteHistoryItem(val packageName: PackageName) : AppSearchAction
    data object ClearHistory : AppSearchAction
    data object FilterClicked : AppSearchAction
    data object ClearAllFilters : AppSearchAction
}
