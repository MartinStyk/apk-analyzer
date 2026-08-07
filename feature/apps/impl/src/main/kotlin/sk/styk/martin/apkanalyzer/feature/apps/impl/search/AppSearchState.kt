package sk.styk.martin.apkanalyzer.feature.apps.impl.search

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.feature.apps.impl.list.AppListItem

@Immutable
data class AppSearchState(
    val query: String = "",
    val results: ImmutableList<AppListItem> = persistentListOf(),
    val searchHistory: ImmutableList<String> = persistentListOf(),
    val totalAppCount: Int = 0,
)
