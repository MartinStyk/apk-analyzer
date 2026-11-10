package sk.styk.martin.apkanalyzer.feature.browse.tv

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
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseDimensionLabeler
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseSubAttribute
import sk.styk.martin.apkanalyzer.core.appindex.browse.browseOptions
import sk.styk.martin.apkanalyzer.core.appindex.browse.bucketsFor
import sk.styk.martin.apkanalyzer.core.appindex.model.AppAttributeIndex
import sk.styk.martin.apkanalyzer.core.appindex.model.AppIndexStatus
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.browse.domain.analytics.BrowseAnalytics
import sk.styk.martin.apkanalyzer.feature.browse.domain.analytics.BrowseAnalyticsEvent
import javax.inject.Inject

@HiltViewModel
internal class TvBrowseViewModel @Inject constructor(
    appIndexRepository: AppIndexRepository,
    installedAppsRepository: InstalledAppsRepository,
    private val labeler: BrowseDimensionLabeler,
    dispatcherProvider: DispatcherProvider,
    private val analytics: BrowseAnalytics,
) : ViewModel() {

    val state: StateFlow<TvBrowseState> = combine(appIndexRepository.index(), installedAppsRepository.apps()) { status, apps ->
        when (status) {
            AppIndexStatus.Loading -> TvBrowseState.Loading

            is AppIndexStatus.Data -> TvBrowseState.Loaded(
                totalApps = apps.size,
                rows = BrowseDimension.entries.map { status.index.row(it, apps.iconRanking()) },
            )
        }
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TvBrowseState.Loading)

    private val eventChannel = Channel<TvBrowseEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        analytics.track(BrowseAnalyticsEvent.TabOpened)
    }

    fun onAction(action: TvBrowseAction) {
        when (action) {
            is TvBrowseAction.SelectDimension -> {
                analytics.track(BrowseAnalyticsEvent.DimensionOpened(action.dimension))
                eventChannel.trySend(TvBrowseEvent.NavigateToDimension(action.dimension))
            }

            is TvBrowseAction.SelectOption -> {
                analytics.track(BrowseAnalyticsEvent.CategoryOpened(action.dimension))
                eventChannel.trySend(
                    TvBrowseEvent.NavigateToApps(
                        dimension = action.dimension,
                        bucketKey = action.option.key,
                        bucketLabel = action.option.label,
                        subAttribute = action.subAttribute,
                    ),
                )
            }
        }
    }

    private fun AppAttributeIndex.row(dimension: BrowseDimension, iconRanking: Map<PackageName, Int>): TvBrowseRow {
        val subAttribute = if (dimension == BrowseDimension.SigningCertificate) BrowseSubAttribute.CertificateOrganization else null
        val buckets = bucketsFor(dimension, subAttribute)
        return TvBrowseRow(
            dimension = dimension,
            subAttribute = subAttribute,
            optionCount = bucketsFor(dimension).size,
            options = browseOptions(dimension, subAttribute, labeler)
                .take(ROW_OPTION_LIMIT)
                .map {
                    TvBrowseRowOption(
                        option = it,
                        packages = buckets[it.key].orEmpty().sortedBy { packageName ->
                            iconRanking[packageName] ?: Int.MAX_VALUE
                        }.take(ROW_ICON_LIMIT),
                    )
                },
        )
    }
}

private const val ROW_OPTION_LIMIT = 4
private const val ROW_ICON_LIMIT = 4

private fun List<InstalledApp>.iconRanking(): Map<PackageName, Int> = sortedWith(
    compareBy<InstalledApp> { it.isSystemApp }
        .thenBy { it.applicationName == it.packageName.value }
        .thenByDescending { it.lastUpdateTime },
)
    .withIndex()
    .associate { (rank, app) -> app.packageName to rank }
