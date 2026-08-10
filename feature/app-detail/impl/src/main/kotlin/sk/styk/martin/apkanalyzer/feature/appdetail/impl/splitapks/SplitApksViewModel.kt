package sk.styk.martin.apkanalyzer.feature.appdetail.impl.splitapks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledSplitApk
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.toAppReference

private const val TAG = "SplitApksViewModel"

@HiltViewModel(assistedFactory = SplitApksViewModel.Factory::class)
internal class SplitApksViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val appDetailRepository: AppDetailRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val clipboardManager: ClipboardManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appDetailInput: AppDetailInput): SplitApksViewModel
    }

    private val source = MutableStateFlow<SplitApksSource>(SplitApksSource.Loading)
    private val query = MutableStateFlow("")

    val state: StateFlow<SplitApksState> = combine(source, query) { source, query ->
        when (source) {
            SplitApksSource.Loading -> SplitApksState.Loading
            SplitApksSource.Error -> SplitApksState.Error
            is SplitApksSource.Ready -> source.filteredBy(query)
        }
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SplitApksState.Loading)

    private val eventChannel = Channel<SplitApksEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadSplits()
    }

    fun onAction(action: SplitApksAction) {
        when (action) {
            SplitApksAction.Retry -> loadSplits()

            SplitApksAction.Back -> eventChannel.trySend(SplitApksEvent.NavigateBack)

            SplitApksAction.ClearQuery -> query.value = ""

            is SplitApksAction.ChangeQuery -> query.value = action.query

            is SplitApksAction.CopyValue -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    viewModelScope.launch { eventChannel.send(SplitApksEvent.ShowCopiedFeedback) }
                }
            }
        }
    }

    private fun loadSplits() {
        source.value = SplitApksSource.Loading
        viewModelScope.launch {
            source.value = withContext(dispatcherProvider.default()) {
                appDetailRepository.details(appDetailInput.toAppReference()).onFailure {
                    Logger.e(TAG, it, "Can not load split APKs for $appDetailInput")
                }.fold(
                    onSuccess = { detail -> SplitApksSource.Ready(detail.info.installedSplits.sortedForDisplay().toImmutableList()) },
                    onFailure = { SplitApksSource.Error },
                )
            }
        }
    }
}

private sealed interface SplitApksSource {
    data object Loading : SplitApksSource
    data object Error : SplitApksSource
    data class Ready(val splits: ImmutableList<InstalledSplitApk>) : SplitApksSource
}

private fun SplitApksSource.Ready.filteredBy(query: String) = SplitApksState.Loaded(
    query = query,
    totalCount = splits.size,
    items = splits.filter { it.matches(query) }.toImmutableList(),
)

private fun InstalledSplitApk.matches(query: String): Boolean {
    if (query.isBlank()) return true
    return fileName.contains(query, ignoreCase = true) || qualifier.contains(query, ignoreCase = true)
}

private fun List<InstalledSplitApk>.sortedForDisplay(): List<InstalledSplitApk> = sortedWith(compareBy({ it.kind.ordinal }, { it.qualifier.lowercase() }))
