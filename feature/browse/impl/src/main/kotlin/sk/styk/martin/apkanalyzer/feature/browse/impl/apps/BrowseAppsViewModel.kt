package sk.styk.martin.apkanalyzer.feature.browse.impl.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
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
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.BrowseSubAttribute
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.bucketsFor
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

@HiltViewModel(assistedFactory = BrowseAppsViewModel.Factory::class)
internal class BrowseAppsViewModel @AssistedInject constructor(
    @Assisted private val dimension: BrowseDimension,
    @Assisted("bucketKey") private val bucketKey: String,
    @Assisted("subAttribute") private val subAttribute: BrowseSubAttribute?,
    appIndexRepository: AppIndexRepository,
    installedAppsRepository: InstalledAppsRepository,
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

    val state: StateFlow<BrowseAppsState> = combine(
        appIndexRepository.index(),
        installedAppsRepository.apps(),
        query,
    ) { status, apps, query -> status.toAppsState(apps, query) }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowseAppsState.Loading)

    private val eventChannel = Channel<BrowseAppsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: BrowseAppsAction) {
        when (action) {
            is BrowseAppsAction.ChangeQuery -> query.value = action.query
            is BrowseAppsAction.AppClicked -> eventChannel.trySend(BrowseAppsEvent.NavigateToAppDetail(action.packageName))
        }
    }

    private fun AppIndexStatus.toAppsState(apps: List<InstalledApp>, query: String): BrowseAppsState = when (this) {
        AppIndexStatus.Loading -> BrowseAppsState.Loading

        is AppIndexStatus.Data -> {
            val bucketPackages = index.bucketsFor(dimension, subAttribute)[bucketKey].orEmpty().toSet()
            val bucketApps = apps
                .filter { it.packageName in bucketPackages }
                .sortedBy { it.applicationName.lowercase() }
                .map { BrowseAppItem(packageName = it.packageName, applicationName = it.applicationName) }

            val filtered = bucketApps.filter { app ->
                query.isBlank() ||
                    app.applicationName.contains(query, ignoreCase = true) ||
                    app.packageName.value.contains(query, ignoreCase = true)
            }

            BrowseAppsState.Loaded(
                query = query,
                totalApps = bucketApps.size,
                apps = filtered.toImmutableList(),
            )
        }
    }
}
