package sk.styk.martin.apkanalyzer.feature.apps.tv.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apps.api.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.api.search.search
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.userpreferences.api.searchhistory.SearchHistoryRepository
import sk.styk.martin.apkanalyzer.feature.apps.tv.components.toTvAppItem
import javax.inject.Inject

@HiltViewModel
internal class TvAppSearchViewModel @Inject constructor(
    installedAppsRepository: InstalledAppsRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val eventChannel = Channel<TvAppSearchEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val searchResultsFlow = combine(installedAppsRepository.apps(), query) { apps, query ->
        apps.search(query).map { it.toTvAppItem() }
    }.flowOn(dispatcherProvider.default())

    private val searchHistoryFlow = searchHistoryRepository.history().map { history ->
        history.mapNotNull { entry ->
            entry.app?.let { app -> TvSearchHistoryItem(app = app.toTvAppItem(), query = entry.query) }
        }
    }.flowOn(dispatcherProvider.default())

    val state: StateFlow<TvAppSearchState> = combine(
        query,
        searchResultsFlow,
        searchHistoryFlow,
    ) { query, results, history ->
        TvAppSearchState(query = query, results = results, history = history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TvAppSearchState())

    fun onAction(action: TvAppSearchAction) {
        when (action) {
            is TvAppSearchAction.QueryChanged -> query.value = action.query
            is TvAppSearchAction.ResultClicked -> openResult(action.packageName)
            is TvAppSearchAction.HistoryItemClicked -> eventChannel.trySend(TvAppSearchEvent.NavigateToAppDetail(action.packageName))
            TvAppSearchAction.ClearHistory -> viewModelScope.launch { searchHistoryRepository.clearAll() }
        }
    }

    private fun openResult(packageName: PackageName) {
        val currentQuery = query.value.trim()
        if (currentQuery.length >= MIN_HISTORY_QUERY_LENGTH) {
            viewModelScope.launch { searchHistoryRepository.addSearch(packageName, currentQuery) }
        }
        eventChannel.trySend(TvAppSearchEvent.NavigateToAppDetail(packageName))
    }
}

private const val MIN_HISTORY_QUERY_LENGTH = 2
