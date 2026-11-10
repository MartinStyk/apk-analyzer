package sk.styk.martin.apkanalyzer.feature.browse.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import sk.styk.martin.apkanalyzer.core.appindex.AppIndexRepository
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseDimensionLabeler
import sk.styk.martin.apkanalyzer.core.appindex.browse.dimensionSummaries
import sk.styk.martin.apkanalyzer.core.appindex.model.AppIndexStatus
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.browse.domain.analytics.BrowseAnalytics
import sk.styk.martin.apkanalyzer.feature.browse.domain.analytics.BrowseAnalyticsEvent
import javax.inject.Inject

private const val TOP_LABELS_COUNT = 6

@HiltViewModel
internal class BrowseViewModel @Inject constructor(
    appIndexRepository: AppIndexRepository,
    installedAppsRepository: InstalledAppsRepository,
    private val labeler: BrowseDimensionLabeler,
    dispatcherProvider: DispatcherProvider,
    private val analytics: BrowseAnalytics,
) : ViewModel() {

    val state: StateFlow<BrowseState> = combine(
        appIndexRepository.index(),
        installedAppsRepository.apps(),
    ) { status, apps -> status.toBrowseState(apps) }
        .flowOn(dispatcherProvider.io())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowseState.Loading)

    private val eventChannel = Channel<BrowseEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        analytics.track(BrowseAnalyticsEvent.TabOpened)
    }

    fun onAction(action: BrowseAction) {
        when (action) {
            is BrowseAction.SelectDimension -> {
                analytics.track(BrowseAnalyticsEvent.DimensionOpened(action.dimension))
                eventChannel.trySend(BrowseEvent.NavigateToDimension(action.dimension))
            }
        }
    }

    private fun AppIndexStatus.toBrowseState(apps: List<InstalledApp>): BrowseState = when (this) {
        AppIndexStatus.Loading -> BrowseState.Loading

        is AppIndexStatus.Data -> BrowseState.Loaded(
            totalApps = apps.size,
            dimensions = index.dimensionSummaries(labeler, TOP_LABELS_COUNT),
        )
    }
}
