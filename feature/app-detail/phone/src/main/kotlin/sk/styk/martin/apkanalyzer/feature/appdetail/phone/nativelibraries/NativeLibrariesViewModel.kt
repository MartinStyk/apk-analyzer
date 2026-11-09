package sk.styk.martin.apkanalyzer.feature.appdetail.phone.nativelibraries

import android.os.Build
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryItem
import sk.styk.martin.apkanalyzer.core.apps.packaging.groupedByLibrary
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSection
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSectionRepository
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.SectionErrorReason
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.isHistorical
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.isOnDevice
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.toSectionErrorReason

@HiltViewModel(assistedFactory = NativeLibrariesViewModel.Factory::class)
internal class NativeLibrariesViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val sectionRepository: AppDetailSectionRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val clipboardManager: ClipboardManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appDetailInput: AppDetailInput): NativeLibrariesViewModel
    }

    private val source = MutableStateFlow<NativeLibrariesSource>(NativeLibrariesSource.Loading)
    private val query = MutableStateFlow("")

    val state: StateFlow<NativeLibrariesState> = combine(source, query) { source, query ->
        when (source) {
            NativeLibrariesSource.Loading -> NativeLibrariesState.Loading
            is NativeLibrariesSource.Error -> NativeLibrariesState.Error(source.reason)
            NativeLibrariesSource.NotRecorded -> NativeLibrariesState.NotRecorded
            is NativeLibrariesSource.Ready -> source.filteredBy(query)
        }
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NativeLibrariesState.Loading)

    private val eventChannel = Channel<NativeLibrariesEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadLibraries()
    }

    fun onAction(action: NativeLibrariesAction) {
        when (action) {
            NativeLibrariesAction.Retry -> loadLibraries()

            NativeLibrariesAction.Back -> eventChannel.trySend(NativeLibrariesEvent.NavigateBack)

            NativeLibrariesAction.ClearQuery -> query.value = ""

            is NativeLibrariesAction.ChangeQuery -> query.value = action.query

            is NativeLibrariesAction.CopyValue -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    viewModelScope.launch { eventChannel.send(NativeLibrariesEvent.ShowCopiedFeedback) }
                }
            }
        }
    }

    private fun loadLibraries() {
        source.value = NativeLibrariesSource.Loading
        viewModelScope.launch {
            source.value = withContext(dispatcherProvider.default()) {
                val deviceAbis = if (appDetailInput.isOnDevice) Build.SUPPORTED_ABIS.toList() else emptyList()
                sectionRepository.nativeLibraries(appDetailInput).fold(
                    onSuccess = { section ->
                        when (section) {
                            AppDetailSection.NotRecorded -> NativeLibrariesSource.NotRecorded

                            is AppDetailSection.Available -> NativeLibrariesSource.Ready(
                                items = section.value.files.groupedByLibrary(deviceAbis.toSet()),
                                isHistorical = appDetailInput.isHistorical,
                            )
                        }
                    },
                    onFailure = { failure -> NativeLibrariesSource.Error(failure.toSectionErrorReason()) },
                )
            }
        }
    }
}

private sealed interface NativeLibrariesSource {
    data object Loading : NativeLibrariesSource
    data class Error(val reason: SectionErrorReason) : NativeLibrariesSource
    data object NotRecorded : NativeLibrariesSource
    data class Ready(val items: List<NativeLibraryItem>, val isHistorical: Boolean) : NativeLibrariesSource
}

private fun NativeLibrariesSource.Ready.filteredBy(query: String) = NativeLibrariesState.Loaded(
    query = query,
    totalCount = items.size,
    items = items.filter { it.matches(query) },
    isHistorical = isHistorical,
)

private fun NativeLibraryItem.matches(query: String): Boolean {
    if (query.isBlank()) return true
    return name.contains(query, ignoreCase = true) || abis.any { it.contains(query, ignoreCase = true) }
}
