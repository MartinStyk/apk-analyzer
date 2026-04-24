package sk.styk.martin.apkanalyzer.feature.apps.impl.list

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
import sk.styk.martin.apkanalyzer.core.applist.model.InstalledApp
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(installedAppsRepository: InstalledAppsRepository, private val recentlyViewedAppsRepository: RecentlyViewedAppsRepository) : ViewModel() {

    private val sortType = MutableStateFlow(SortType.Name)
    private val sortAscending = MutableStateFlow(true)

    private val eventChannel = Channel<AppsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val appsFlow = installedAppsRepository.apps()
        .map { apps ->
            AppListState.Content(apps = apps.map { it.toListItem() }.toImmutableList())
        }

    private val recentsFlow = recentlyViewedAppsRepository.recents()
        .map { apps ->
            if (apps.isEmpty()) {
                RecentsState.NoRecents
            } else {
                RecentsState.Content(apps.map { it.toListItem() }.toImmutableList())
            }
        }

    val state = combine(
        appsFlow,
        recentsFlow,
        sortType,
        sortAscending,
    ) { apps, recents, sort, ascending ->
        val sortedApps = apps.copy(
            apps = apps.apps.sortedWith(sort.comparator(ascending)).toImmutableList(),
        )
        AppsState(
            apps = sortedApps,
            recents = recents,
            sortType = sort,
            sortAscending = ascending,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppsState())

    fun onAction(action: AppsAction) {
        when (action) {
            is AppsAction.SortTypeSelected -> {
                if (sortType.value == action.sortType) {
                    sortAscending.value = !sortAscending.value
                } else {
                    sortType.value = action.sortType
                    sortAscending.value = true
                }
            }

            is AppsAction.AppClicked -> {
                viewModelScope.launch { recentlyViewedAppsRepository.addRecent(action.packageName) }
                eventChannel.trySend(AppsEvent.NavigateToAppDetail(action.packageName))
            }

            is AppsAction.SearchClicked -> {
                eventChannel.trySend(AppsEvent.NavigateToSearch)
            }
        }
    }

    private fun SortType.comparator(ascending: Boolean): Comparator<AppListItem> {
        val base: Comparator<AppListItem> = when (this) {
            SortType.Name -> {
                val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.SECONDARY }
                Comparator { a, b -> collator.compare(a.applicationName, b.applicationName) }
            }

            SortType.Size -> compareBy { it.apkSize }

            SortType.InstallDate -> compareBy { it.installTime }

            SortType.TargetSdk -> compareBy { it.targetSdk }
        }
        return if (ascending) base else base.reversed()
    }

    private fun InstalledApp.toListItem() = AppListItem(
        packageName = packageName,
        applicationName = applicationName,
        targetSdk = targetSdk,
        apkSize = apkSize,
        installTime = installTime,
    )
}
