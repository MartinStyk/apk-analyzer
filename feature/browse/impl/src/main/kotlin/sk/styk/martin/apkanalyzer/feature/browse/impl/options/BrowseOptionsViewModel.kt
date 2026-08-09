package sk.styk.martin.apkanalyzer.feature.browse.impl.options

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.appindex.AppIndexRepository
import sk.styk.martin.apkanalyzer.core.appindex.model.AppAttributeIndex
import sk.styk.martin.apkanalyzer.core.appindex.model.AppIndexStatus
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.BrowseDimensionLabeler
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.BrowseSubAttribute
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.bucketsFor
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.isCertificateHash
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.subAttributes
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

@HiltViewModel(assistedFactory = BrowseOptionsViewModel.Factory::class)
internal class BrowseOptionsViewModel @AssistedInject constructor(
    @Assisted private val dimension: BrowseDimension,
    appIndexRepository: AppIndexRepository,
    private val labeler: BrowseDimensionLabeler,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(dimension: BrowseDimension): BrowseOptionsViewModel
    }

    private val subAttributes: List<BrowseSubAttribute?> =
        dimension.subAttributes().takeIf { it.isNotEmpty() } ?: listOf(null)

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
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowseOptionsState.Loading)

    private val eventChannel = Channel<BrowseOptionsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: BrowseOptionsAction) {
        when (action) {
            is BrowseOptionsAction.ChangeQuery -> narrowing.update { it.copy(query = action.query) }

            is BrowseOptionsAction.SelectSubAttribute -> narrowing.update { it.copy(subAttribute = action.subAttribute) }

            is BrowseOptionsAction.SelectOption -> {
                val loaded = state.value as? BrowseOptionsState.Loaded
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
            optionsBySubAttribute = subAttributes.associateWith { subAttribute -> index.buildOptions(subAttribute) },
        )
    }

    private fun AppAttributeIndex.buildOptions(subAttribute: BrowseSubAttribute?): List<BrowseOption> = bucketsFor(dimension, subAttribute)
        .map { (key, packages) ->
            val label = labeler.label(dimension, key, subAttribute)
            if (subAttribute.isCertificateHash) {
                BrowseOption.CertificateHash(
                    key = key,
                    label = label,
                    algorithm = requireNotNull(subAttribute),
                    count = packages.size,
                )
            } else {
                BrowseOption.Labeled(
                    key = key,
                    label = label,
                    rawIdentifier = labeler.rawIdentifier(dimension, key, subAttribute),
                    count = packages.size,
                )
            }
        }
        .sortedWith(compareByDescending<BrowseOption> { it.count }.thenBy { it.label })
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
        options = filtered.toImmutableList(),
    )
}

private fun BrowseOption.matches(query: String): Boolean = query.isBlank() ||
    key.contains(query, ignoreCase = true) ||
    label.contains(query, ignoreCase = true) ||
    (this as? BrowseOption.Labeled)?.rawIdentifier?.contains(query, ignoreCase = true) == true
