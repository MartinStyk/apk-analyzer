package sk.styk.martin.apkanalyzer.feature.browse.tv.options

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
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.appindex.AppIndexRepository
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseBucketDetailResolver
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseDimensionLabeler
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseOption
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseSubAttribute
import sk.styk.martin.apkanalyzer.core.appindex.browse.browseOptions
import sk.styk.martin.apkanalyzer.core.appindex.browse.bucketsFor
import sk.styk.martin.apkanalyzer.core.appindex.browse.matches
import sk.styk.martin.apkanalyzer.core.appindex.browse.subAttributes
import sk.styk.martin.apkanalyzer.core.appindex.browse.subAttributesOrDefault
import sk.styk.martin.apkanalyzer.core.appindex.model.AppAttributeIndex
import sk.styk.martin.apkanalyzer.core.appindex.model.AppIndexStatus
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.browse.tv.apps.TvBrowseAppItem

@HiltViewModel(assistedFactory = TvBrowseOptionsViewModel.Factory::class)
internal class TvBrowseOptionsViewModel @AssistedInject constructor(
    @Assisted private val dimension: BrowseDimension,
    appIndexRepository: AppIndexRepository,
    installedAppsRepository: InstalledAppsRepository,
    private val labeler: BrowseDimensionLabeler,
    private val bucketDetailResolver: BrowseBucketDetailResolver,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(dimension: BrowseDimension): TvBrowseOptionsViewModel
    }

    private val narrowing = MutableStateFlow(Narrowing())

    private val source = combine(appIndexRepository.index(), installedAppsRepository.apps()) { status, apps ->
        when (status) {
            AppIndexStatus.Loading -> OptionsSource.Loading

            is AppIndexStatus.Data -> OptionsSource.Ready(
                index = status.index,
                optionsBySubAttribute = dimension.subAttributesOrDefault().associateWith { status.index.browseOptions(dimension, it, labeler) },
                appNames = apps.associate { it.packageName to it.applicationName },
            )
        }
    }
        .flowOn(dispatcherProvider.io())

    val state: StateFlow<TvBrowseOptionsState> = combine(source, narrowing) { source, narrowing ->
        when (source) {
            OptionsSource.Loading -> TvBrowseOptionsState.Loading
            is OptionsSource.Ready -> narrowedState(source, narrowing)
        }
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TvBrowseOptionsState.Loading)

    private val eventChannel = Channel<TvBrowseOptionsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: TvBrowseOptionsAction) {
        when (action) {
            is TvBrowseOptionsAction.ChangeQuery -> narrowing.update { it.copy(query = action.query) }

            is TvBrowseOptionsAction.SelectSubAttribute -> narrowing.update { it.copy(subAttribute = action.subAttribute) }

            is TvBrowseOptionsAction.FocusOption -> narrowing.update { it.copy(focusedKey = action.option.key) }

            is TvBrowseOptionsAction.SelectOption -> eventChannel.trySend(
                TvBrowseOptionsEvent.NavigateToApps(
                    bucketKey = action.option.key,
                    bucketLabel = action.option.label,
                    subAttribute = (state.value as? TvBrowseOptionsState.Loaded)?.subAttribute,
                ),
            )
        }
    }

    private fun narrowedState(source: OptionsSource.Ready, narrowing: Narrowing): TvBrowseOptionsState.Loaded {
        val subAttribute = narrowing.subAttribute?.takeIf { it in source.optionsBySubAttribute } ?: source.optionsBySubAttribute.keys.firstOrNull()
        val allOptions = source.optionsBySubAttribute[subAttribute].orEmpty()
        val options = allOptions.filter { it.matches(narrowing.query) }
        val previewed = options.firstOrNull { it.key == narrowing.focusedKey } ?: options.firstOrNull()
        return TvBrowseOptionsState.Loaded(
            query = narrowing.query,
            subAttribute = subAttribute,
            subAttributes = dimension.subAttributes(),
            totalOptions = allOptions.size,
            options = options,
            preview = previewed?.let { preview(source, it, subAttribute) },
        )
    }

    private fun preview(
        source: OptionsSource.Ready,
        option: BrowseOption,
        subAttribute: BrowseSubAttribute?,
    ): TvBrowseOptionPreview {
        val apps = source.index.bucketsFor(dimension, subAttribute)[option.key].orEmpty()
            .mapNotNull { packageName -> source.appNames[packageName]?.let { TvBrowseAppItem(packageName = packageName, applicationName = it) } }
            .sortedBy { it.applicationName.lowercase() }
        return TvBrowseOptionPreview(
            option = option,
            detail = bucketDetailResolver.resolve(source.index, dimension, option.key, subAttribute),
            apps = apps.take(PREVIEW_APP_LIMIT),
            totalApps = apps.size,
        )
    }
}

private const val PREVIEW_APP_LIMIT = 12

private data class Narrowing(
    val query: String = "",
    val subAttribute: BrowseSubAttribute? = null,
    val focusedKey: String? = null,
)

private sealed interface OptionsSource {
    data object Loading : OptionsSource

    data class Ready(
        val index: AppAttributeIndex,
        val optionsBySubAttribute: Map<BrowseSubAttribute?, List<BrowseOption>>,
        val appNames: Map<PackageName, String>,
    ) : OptionsSource
}
