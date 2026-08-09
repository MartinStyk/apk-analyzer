package sk.styk.martin.apkanalyzer.feature.apps.impl.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
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
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.userpreferences.SearchHistoryRepository
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterRepository
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.FilterAppsUseCase
import sk.styk.martin.apkanalyzer.feature.apps.impl.list.AppListItem
import sk.styk.martin.apkanalyzer.feature.apps.impl.search.domain.SearchAppsUseCase
import javax.inject.Inject

@HiltViewModel
class AppSearchViewModel @Inject constructor(
    installedAppsRepository: InstalledAppsRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val appFilterRepository: AppFilterRepository,
    filterApps: FilterAppsUseCase,
    searchApps: SearchAppsUseCase,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val eventChannel = Channel<AppSearchEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val allApps = installedAppsRepository.apps()

    private val filteredAppsFlow = combine(allApps, appFilterRepository.filter) { apps, filter ->
        FilteredSearchApps(
            items = filterApps(apps, filter).map { it.toListItem() }.toImmutableList(),
            totalCount = apps.size,
        )
    }.flowOn(dispatcherProvider.default())

    private val searchResultsFlow = combine(filteredAppsFlow, query) { (apps, _), query ->
        searchApps(query, apps).toImmutableList()
    }.flowOn(dispatcherProvider.default())

    val state = combine(
        query,
        searchResultsFlow,
        allApps.map { it.size }.distinctUntilChanged(),
        searchHistoryRepository.queries(),
    ) { query, results, totalCount, history ->
        AppSearchState(
            query = query,
            results = results,
            searchHistory = history.toImmutableList(),
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
                        searchHistoryRepository.addQuery(currentQuery)
                    }
                }
                eventChannel.trySend(AppSearchEvent.NavigateToAppDetail(action.app.packageName))
            }

            is AppSearchAction.HistoryQueryClicked -> query.value = action.query

            is AppSearchAction.DeleteHistoryItem -> {
                viewModelScope.launch { searchHistoryRepository.removeQuery(action.query) }
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

    private fun InstalledApp.toListItem() = AppListItem(
        packageName = packageName,
        applicationName = applicationName,
        targetSdk = targetSdk,
        apkSize = apkSize,
        totalSize = totalSize,
        installTime = installTime,
        lastUpdateTime = lastUpdateTime,
        lastUsedTime = lastUsedTime,
        source = installSourceChain.source,
    )

    private companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}

private data class FilteredSearchApps(val items: ImmutableList<AppListItem>, val totalCount: Int)
