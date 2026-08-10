package sk.styk.martin.apkanalyzer.feature.appdetail.impl.manifest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.manifest.ManifestParser
import sk.styk.martin.apkanalyzer.core.apps.manifest.ParsedManifest
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.toAppReference

private const val TAG = "ManifestViewModel"

@HiltViewModel(assistedFactory = ManifestViewModel.Factory::class)
internal class ManifestViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val manifestParser: ManifestParser,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(target: AppDetailInput): ManifestViewModel
    }

    private val source = MutableStateFlow<ManifestSource>(ManifestSource.Loading)
    private val query = MutableStateFlow("")

    val state: StateFlow<ManifestState> = combine(source, query) { source, query ->
        when (source) {
            ManifestSource.Loading -> ManifestState.Loading
            ManifestSource.Error -> ManifestState.Error
            is ManifestSource.Ready -> source.filteredBy(query)
        }
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManifestState.Loading)

    init {
        loadManifest()
    }

    fun onAction(action: ManifestAction) {
        when (action) {
            ManifestAction.Retry -> loadManifest()
            is ManifestAction.ChangeQuery -> query.value = action.query
        }
    }

    private fun loadManifest() {
        source.value = ManifestSource.Loading
        query.value = ""
        viewModelScope.launch {
            val result = manifestParser.manifest(appDetailInput.toAppReference())
            source.value = withContext(dispatcherProvider.default()) {
                result.onFailure {
                    Logger.e(TAG, it, "Can not load manifest for $appDetailInput")
                }.fold(
                    onSuccess = { it.toSource() },
                    onFailure = { ManifestSource.Error },
                )
            }
        }
    }
}

private sealed interface ManifestSource {
    data object Loading : ManifestSource
    data object Error : ManifestSource
    data class Ready(val lines: ImmutableList<String>, val additionalInstalledSplits: Int) : ManifestSource
}

private fun ParsedManifest.toSource() = ManifestSource.Ready(
    lines = xml.lineSequence().toImmutableList(),
    additionalInstalledSplits = additionalInstalledSplits,
)

private fun ManifestSource.Ready.filteredBy(query: String): ManifestState.Loaded {
    val matchingIndices = if (query.isBlank()) {
        lines.indices.toList()
    } else {
        lines.indices.filter { index -> lines[index].contains(query, ignoreCase = true) }
    }
    val displayedIndices = if (query.isBlank()) {
        matchingIndices
    } else {
        buildSet {
            matchingIndices.forEach { index ->
                addAll(lines.ownerElementRange(index))
                add(index)
            }
        }.sorted()
    }
    val matchingIndexSet = matchingIndices.toSet()
    return ManifestState.Loaded(
        query = query,
        displayedLines = displayedIndices.map { index ->
            ManifestLine(
                number = index + 1,
                text = lines[index],
                isMatch = query.isNotBlank() && index in matchingIndexSet,
            )
        }.toImmutableList(),
        lineCount = lines.size,
        matchCount = if (query.isBlank()) 0 else matchingIndices.size,
        additionalInstalledSplits = additionalInstalledSplits,
    )
}

private fun List<String>.ownerElementRange(index: Int): IntRange {
    val matchIndent = get(index).leadingSpaceCount()
    val matchingLine = get(index).trimStart()
    val ownerIndex = if (matchingLine.startsWith("<") && !matchingLine.startsWith("</")) {
        index
    } else {
        (index downTo 0).firstOrNull { candidateIndex ->
            val candidate = get(candidateIndex)
            val trimmed = candidate.trimStart()
            candidate.leadingSpaceCount() < matchIndent &&
                trimmed.startsWith("<") &&
                !trimmed.startsWith("</")
        }
    } ?: return index..index
    val endIndex = (ownerIndex until size).firstOrNull { candidateIndex ->
        get(candidateIndex).trimEnd().endsWith(">")
    } ?: index
    return ownerIndex..endIndex
}

private fun String.leadingSpaceCount(): Int = indexOfFirst { !it.isWhitespace() }
    .takeIf { it >= 0 }
    ?: length
