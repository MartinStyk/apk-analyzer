package sk.styk.martin.apkanalyzer.feature.apps.impl.search

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.apps.impl.list.AppListItem

@Immutable
data class AppSearchState(
    val query: String = "",
    val results: List<AppListItem> = listOf(),
    val searchHistory: List<SearchHistoryItem> = listOf(),
    val totalAppCount: Int = 0,
)

@Immutable
data class SearchHistoryItem(
    val packageName: PackageName,
    val query: String,
    val app: AppListItem?,
)
