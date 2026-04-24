package sk.styk.martin.apkanalyzer.feature.apps.impl.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.applist.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.applist.RecentlyViewedAppsRepository
import sk.styk.martin.apkanalyzer.core.applist.SearchHistoryRepository
import sk.styk.martin.apkanalyzer.core.applist.model.InstalledApp
import sk.styk.martin.apkanalyzer.feature.apps.impl.list.AppListItem
import javax.inject.Inject

@HiltViewModel
class AppSearchViewModel @Inject constructor(installedAppsRepository: InstalledAppsRepository, private val recentlyViewedAppsRepository: RecentlyViewedAppsRepository, private val searchHistoryRepository: SearchHistoryRepository) :
    ViewModel() {

    private val query = MutableStateFlow("")

    private val eventChannel = Channel<AppSearchEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val allApps = installedAppsRepository.apps()
        .map { apps -> apps.map { it.toListItem() } }

    val state = combine(allApps, query, searchHistoryRepository.queries()) { apps, q, history ->
        val results = if (q.isBlank()) {
            emptyList()
        } else {
            apps.filter {
                it.applicationName.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true)
            }
        }
        AppSearchState(
            query = q,
            results = results.toImmutableList(),
            searchHistory = history.toImmutableList(),
            totalAppCount = apps.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSearchState())

    fun onAction(action: AppSearchAction) {
        when (action) {
            is AppSearchAction.QueryChanged -> query.value = action.query

            is AppSearchAction.AppClicked -> {
                val currentQuery = query.value
                viewModelScope.launch {
                    recentlyViewedAppsRepository.addRecent(action.packageName)
                    if (currentQuery.length >= MIN_QUERY_LENGTH) {
                        searchHistoryRepository.addQuery(currentQuery)
                    }
                }
                eventChannel.trySend(AppSearchEvent.NavigateToAppDetail(action.packageName))
            }

            is AppSearchAction.HistoryQueryClicked -> query.value = action.query

            is AppSearchAction.DeleteHistoryItem -> {
                viewModelScope.launch { searchHistoryRepository.removeQuery(action.query) }
            }

            is AppSearchAction.ClearHistory -> {
                viewModelScope.launch { searchHistoryRepository.clearAll() }
            }
        }
    }

    private fun InstalledApp.toListItem() = AppListItem(
        packageName = packageName,
        applicationName = applicationName,
        targetSdk = targetSdk,
        apkSize = apkSize,
        installTime = installTime,
    )

    private companion object {
        const val MIN_QUERY_LENGTH = 3
    }
}
