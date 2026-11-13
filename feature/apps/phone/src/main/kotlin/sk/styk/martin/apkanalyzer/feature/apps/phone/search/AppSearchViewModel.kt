package sk.styk.martin.apkanalyzer.feature.apps.phone.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.filtering.AppFilterRepository
import sk.styk.martin.apkanalyzer.core.apps.filtering.filterBy
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.search.search
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.userpreferences.searchhistory.SearchHistoryRepository
import sk.styk.martin.apkanalyzer.feature.apps.phone.list.toListItem
import javax.inject.Inject

@HiltViewModel
internal class AppSearchViewModel @Inject constructor(
    installedAppsRepository: InstalledAppsRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val appFilterRepository: AppFilterRepository,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val eventChannel = Channel<AppSearchEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val allApps = installedAppsRepository.apps()

    private val filteredAppsFlow = combine(allApps, appFilterRepository.filter) { apps, filter ->
        FilteredSearchApps(
            items = apps.filterBy(filter),
            totalCount = apps.size,
        )
    }.flowOn(dispatcherProvider.default())

    private val searchResultsFlow = combine(filteredAppsFlow, query) { (apps, _), query ->
        apps.search(query).map { it.toListItem() }
    }.flowOn(dispatcherProvider.default())

    private val searchHistoryFlow = searchHistoryRepository.history().map { entries ->
        entries.map { entry ->
            SearchHistoryItem(
                packageName = entry.packageName,
                query = entry.query,
                app = entry.app?.toListItem(),
            )
        }
    }.flowOn(dispatcherProvider.default())

    val state = combine(
        query,
        searchResultsFlow,
        allApps.map { it.size }.distinctUntilChanged(),
        searchHistoryFlow,
    ) { query, results, totalCount, history ->
        AppSearchState(
            query = query,
            results = results,
            searchHistory = history,
            totalAppCount = totalCount,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSearchState())

    fun onAction(action: AppSearchAction) {
        when (action) {
            is AppSearchAction.QueryChanged -> query.value = action.query

            is AppSearchAction.AppClicked -> {
                val currentQuery = query.value
                viewModelScope.launch {
                    if (currentQuery.length >= MIN_QUERY_LENGTH) {
                        searchHistoryRepository.addSearch(action.app.packageName, currentQuery)
                    }
                }
                eventChannel.trySend(AppSearchEvent.NavigateToAppDetail(action.app.packageName))
            }

            is AppSearchAction.HistoryItemClicked -> {
                val app = action.item.app
                if (app != null) {
                    eventChannel.trySend(AppSearchEvent.NavigateToAppDetail(app.packageName))
                } else {
                    query.value = action.item.query
                }
            }

            is AppSearchAction.DeleteHistoryItem -> {
                viewModelScope.launch { searchHistoryRepository.removeEntry(action.packageName) }
            }

            is AppSearchAction.ClearHistory -> {
                viewModelScope.launch { searchHistoryRepository.clearAll() }
            }

            is AppSearchAction.FilterClicked -> {
                eventChannel.trySend(AppSearchEvent.NavigateToFilter)
            }

            is AppSearchAction.ClearAllFilters -> {
                query.value = ""
                appFilterRepository.clear()
            }
        }
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}

private data class FilteredSearchApps(val items: List<InstalledApp>, val totalCount: Int)
