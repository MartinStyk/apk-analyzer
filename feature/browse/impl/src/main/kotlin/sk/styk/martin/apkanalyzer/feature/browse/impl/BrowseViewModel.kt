package sk.styk.martin.apkanalyzer.feature.browse.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import sk.styk.martin.apkanalyzer.core.appindex.AppIndexRepository
import sk.styk.martin.apkanalyzer.core.appindex.model.AppIndexStatus
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.BrowseDimensionLabeler
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.CERTIFICATE_ORGANIZATION
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.bucketsFor
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension
import javax.inject.Inject

private const val TOP_LABELS_COUNT = 4

@HiltViewModel
internal class BrowseViewModel @Inject constructor(
    appIndexRepository: AppIndexRepository,
    installedAppsRepository: InstalledAppsRepository,
    private val labeler: BrowseDimensionLabeler,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    val state: StateFlow<BrowseState> = combine(
        appIndexRepository.index(),
        installedAppsRepository.apps(),
    ) { status, apps -> status.toBrowseState(apps) }
        .flowOn(dispatcherProvider.io())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowseState.Loading)

    private val eventChannel = Channel<BrowseEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: BrowseAction) {
        when (action) {
            is BrowseAction.SelectDimension -> eventChannel.trySend(BrowseEvent.NavigateToDimension(action.dimension))
        }
    }

    private fun AppIndexStatus.toBrowseState(apps: List<InstalledApp>): BrowseState = when (this) {
        AppIndexStatus.Loading -> BrowseState.Loading

        is AppIndexStatus.Data -> BrowseState.Loaded(
            totalApps = apps.size,
            dimensions = BrowseDimension.entries.map { dimension ->
                val previewSubKey = if (dimension == BrowseDimension.SigningCertificate) CERTIFICATE_ORGANIZATION else null
                val buckets = index.bucketsFor(dimension, previewSubKey)
                DimensionSummary(
                    dimension = dimension,
                    optionCount = index.bucketsFor(dimension).size,
                    topLabels = buckets.entries
                        .sortedByDescending { it.value.size }
                        .take(TOP_LABELS_COUNT)
                        .map { labeler.label(dimension, it.key, previewSubKey) }
                        .toImmutableList(),
                )
            }.toImmutableList(),
        )
    }
}
