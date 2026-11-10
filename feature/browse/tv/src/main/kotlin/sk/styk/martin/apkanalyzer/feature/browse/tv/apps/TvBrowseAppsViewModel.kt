package sk.styk.martin.apkanalyzer.feature.browse.tv.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import sk.styk.martin.apkanalyzer.core.appindex.AppIndexRepository
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseBucketDetail
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseBucketDetailResolver
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseSubAttribute
import sk.styk.martin.apkanalyzer.core.appindex.browse.bucketsFor
import sk.styk.martin.apkanalyzer.core.appindex.model.AppIndexStatus
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider

@HiltViewModel(assistedFactory = TvBrowseAppsViewModel.Factory::class)
internal class TvBrowseAppsViewModel @AssistedInject constructor(
    @Assisted private val dimension: BrowseDimension,
    @Assisted("bucketKey") private val bucketKey: String,
    @Assisted("subAttribute") private val subAttribute: BrowseSubAttribute?,
    appIndexRepository: AppIndexRepository,
    installedAppsRepository: InstalledAppsRepository,
    bucketDetailResolver: BrowseBucketDetailResolver,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            dimension: BrowseDimension,
            @Assisted("bucketKey") bucketKey: String,
            @Assisted("subAttribute") subAttribute: BrowseSubAttribute?,
        ): TvBrowseAppsViewModel
    }

    private val query = MutableStateFlow("")

    private val bucket = combine(appIndexRepository.index(), installedAppsRepository.apps()) { status, apps ->
        when (status) {
            AppIndexStatus.Loading -> BucketApps.Loading

            is AppIndexStatus.Data -> {
                val packages = status.index.bucketsFor(dimension, subAttribute)[bucketKey].orEmpty().toSet()
                BucketApps.Data(
                    apps = apps
                        .filter { it.packageName in packages }
                        .sortedBy { it.applicationName.lowercase() }
                        .map { TvBrowseAppItem(packageName = it.packageName, applicationName = it.applicationName) },
                    bucketDetail = bucketDetailResolver.resolve(status.index, dimension, bucketKey, subAttribute),
                )
            }
        }
    }
        .flowOn(dispatcherProvider.default())

    val state: StateFlow<TvBrowseAppsState> = combine(bucket, query) { bucket, query ->
        when (bucket) {
            BucketApps.Loading -> TvBrowseAppsState.Loading

            is BucketApps.Data -> TvBrowseAppsState.Loaded(
                query = query,
                totalApps = bucket.apps.size,
                apps = bucket.apps.filter { it.matches(query) },
                bucketDetail = bucket.bucketDetail,
            )
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TvBrowseAppsState.Loading)

    private val eventChannel = Channel<TvBrowseAppsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: TvBrowseAppsAction) {
        when (action) {
            is TvBrowseAppsAction.ChangeQuery -> query.value = action.query
            is TvBrowseAppsAction.SelectApp -> eventChannel.trySend(TvBrowseAppsEvent.NavigateToAppDetail(action.packageName))
        }
    }
}

private sealed interface BucketApps {
    data object Loading : BucketApps

    data class Data(val apps: List<TvBrowseAppItem>, val bucketDetail: BrowseBucketDetail?) : BucketApps
}

private fun TvBrowseAppItem.matches(query: String): Boolean = query.isBlank() ||
    applicationName.contains(query, ignoreCase = true) ||
    packageName.value.contains(query, ignoreCase = true)
