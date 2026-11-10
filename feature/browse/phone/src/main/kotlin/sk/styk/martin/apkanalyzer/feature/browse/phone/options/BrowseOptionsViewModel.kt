package sk.styk.martin.apkanalyzer.feature.browse.phone.options

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.appindex.AppIndexRepository
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseDimensionLabeler
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseOption
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseSubAttribute
import sk.styk.martin.apkanalyzer.core.appindex.browse.browseOptions
import sk.styk.martin.apkanalyzer.core.appindex.browse.matches
import sk.styk.martin.apkanalyzer.core.appindex.browse.subAttributesOrDefault
import sk.styk.martin.apkanalyzer.core.appindex.model.AppIndexStatus
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.browse.phone.BrowseAnalytics
import sk.styk.martin.apkanalyzer.feature.browse.phone.BrowseAnalyticsEvent

@HiltViewModel(assistedFactory = BrowseOptionsViewModel.Factory::class)
internal class BrowseOptionsViewModel @AssistedInject constructor(
    @Assisted private val dimension: BrowseDimension,
    appIndexRepository: AppIndexRepository,
    private val labeler: BrowseDimensionLabeler,
    dispatcherProvider: DispatcherProvider,
    private val analytics: BrowseAnalytics,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(dimension: BrowseDimension): BrowseOptionsViewModel
    }

    private val subAttributes: List<BrowseSubAttribute?> = dimension.subAttributesOrDefault()

    private val narrowing = MutableStateFlow(Narrowing())

    private val source = appIndexRepository.index()
        .map { it.toSource() }
        .flowOn(dispatcherProvider.io())

    val state: StateFlow<BrowseOptionsState> = combine(source, narrowing) { source, narrowing ->
        when (source) {
            OptionsSource.Loading -> BrowseOptionsState.Loading
            is OptionsSource.Ready -> source.narrowedBy(narrowing)
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowseOptionsState.Loading)

    private val eventChannel = Channel<BrowseOptionsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: BrowseOptionsAction) {
        when (action) {
            is BrowseOptionsAction.ChangeQuery -> narrowing.update { it.copy(query = action.query) }

            is BrowseOptionsAction.SelectSubAttribute -> narrowing.update { it.copy(subAttribute = action.subAttribute) }

            is BrowseOptionsAction.SelectOption -> {
                val loaded = state.value as? BrowseOptionsState.Loaded
                analytics.track(BrowseAnalyticsEvent.CategoryOpened(dimension))
                eventChannel.trySend(
                    BrowseOptionsEvent.NavigateToApps(
                        bucketKey = action.option.key,
                        bucketLabel = action.option.label,
                        subAttribute = loaded?.subAttribute,
                    ),
                )
            }
        }
    }

    private fun AppIndexStatus.toSource(): OptionsSource = when (this) {
        AppIndexStatus.Loading -> OptionsSource.Loading

        is AppIndexStatus.Data -> OptionsSource.Ready(
            optionsBySubAttribute = subAttributes.associateWith { subAttribute -> index.browseOptions(dimension, subAttribute, labeler) },
        )
    }
}

private data class Narrowing(val query: String = "", val subAttribute: BrowseSubAttribute? = null)

private sealed interface OptionsSource {
    data object Loading : OptionsSource

    data class Ready(val optionsBySubAttribute: Map<BrowseSubAttribute?, List<BrowseOption>>) : OptionsSource
}

private fun OptionsSource.Ready.narrowedBy(narrowing: Narrowing): BrowseOptionsState.Loaded {
    val effectiveSubAttribute = narrowing.subAttribute?.takeIf { it in optionsBySubAttribute } ?: optionsBySubAttribute.keys.firstOrNull()
    val allOptions = optionsBySubAttribute[effectiveSubAttribute].orEmpty()
    val filtered = allOptions.filter { it.matches(narrowing.query) }

    return BrowseOptionsState.Loaded(
        query = narrowing.query,
        subAttribute = effectiveSubAttribute,
        totalOptions = allOptions.size,
        options = filtered,
    )
}
