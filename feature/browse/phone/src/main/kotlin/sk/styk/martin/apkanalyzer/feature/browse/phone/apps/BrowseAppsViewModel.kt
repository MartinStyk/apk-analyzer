package sk.styk.martin.apkanalyzer.feature.browse.phone.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
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
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

@HiltViewModel(assistedFactory = BrowseAppsViewModel.Factory::class)
internal class BrowseAppsViewModel @AssistedInject constructor(
    @Assisted private val dimension: BrowseDimension,
    @Assisted("bucketKey") private val bucketKey: String,
    @Assisted("subAttribute") private val subAttribute: BrowseSubAttribute?,
    appIndexRepository: AppIndexRepository,
    installedAppsRepository: InstalledAppsRepository,
    private val bucketDetailResolver: BrowseBucketDetailResolver,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            dimension: BrowseDimension,
            @Assisted("bucketKey") bucketKey: String,
            @Assisted("subAttribute") subAttribute: BrowseSubAttribute?,
        ): BrowseAppsViewModel
    }

    private val query = MutableStateFlow("")

    private val bucket: Flow<BucketApps> = combine(
        appIndexRepository.index(),
        installedAppsRepository.apps(),
    ) { status, apps -> status.toBucketApps(apps) }
        .flowOn(dispatcherProvider.default())

    val state: StateFlow<BrowseAppsState> = combine(bucket, query) { bucket, query -> bucket.toAppsState(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowseAppsState.Loading)

    private val eventChannel = Channel<BrowseAppsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: BrowseAppsAction) {
        when (action) {
            is BrowseAppsAction.ChangeQuery -> query.value = action.query
            is BrowseAppsAction.AppClicked -> eventChannel.trySend(BrowseAppsEvent.NavigateToAppDetail(action.packageName))
        }
    }

    private fun AppIndexStatus.toBucketApps(apps: List<InstalledApp>): BucketApps = when (this) {
        AppIndexStatus.Loading -> BucketApps.Loading

        is AppIndexStatus.Data -> {
            val bucketPackages = index.bucketsFor(dimension, subAttribute)[bucketKey].orEmpty().toSet()
            val bucketApps = apps
                .filter { it.packageName in bucketPackages }
                .sortedBy { it.applicationName.lowercase() }
                .map { BrowseAppItem(packageName = it.packageName, applicationName = it.applicationName) }

            BucketApps.Data(apps = bucketApps, bucketDetail = bucketDetailResolver.resolve(index, dimension, bucketKey, subAttribute))
        }
    }

    private fun BucketApps.toAppsState(query: String): BrowseAppsState = when (this) {
        BucketApps.Loading -> BrowseAppsState.Loading

        is BucketApps.Data -> {
            val filtered = apps.filter { app ->
                query.isBlank() ||
                    app.applicationName.contains(query, ignoreCase = true) ||
                    app.packageName.value.contains(query, ignoreCase = true)
            }

            BrowseAppsState.Loaded(
                query = query,
                totalApps = apps.size,
                apps = filtered,
                bucketDetail = bucketDetail,
            )
        }
    }
}

private sealed interface BucketApps {
    data object Loading : BucketApps

    data class Data(val apps: List<BrowseAppItem>, val bucketDetail: BrowseBucketDetail?) : BucketApps
}
