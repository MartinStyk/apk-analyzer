package sk.styk.martin.apkanalyzer.feature.appdetail.impl.storage

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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.storagestats.StorageBreakdown
import sk.styk.martin.apkanalyzer.core.apps.storagestats.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput

private const val STOP_TIMEOUT_MS = 5000L

@HiltViewModel(assistedFactory = StorageViewModel.Factory::class)
internal class StorageViewModel @AssistedInject constructor(
    @Assisted appDetailInput: AppDetailInput,
    private val storageStatsRepository: StorageStatsRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val clipboardManager: ClipboardManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appDetailInput: AppDetailInput): StorageViewModel
    }

    private val packageName = (appDetailInput as? AppDetailInput.InstalledPackage)?.let { PackageName(it.packageName) }
    private val isLoading = MutableStateFlow(true)
    private val breakdown = MutableStateFlow<StorageBreakdown?>(null)

    val state: StateFlow<StorageState> = combine(
        isLoading,
        breakdown,
        storageStatsRepository.isPermissionGranted,
    ) { loading, breakdown, permissionGranted ->
        when {
            loading -> StorageState.Loading
            !permissionGranted -> StorageState.MissingPermission
            breakdown != null -> StorageState.Loaded(breakdown)
            else -> StorageState.Error
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StorageState.Loading)

    private val eventChannel = Channel<StorageEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadBreakdown()
        storageStatsRepository.isPermissionGranted
            .drop(1)
            .filter { it }
            .onEach { loadBreakdown() }
            .launchIn(viewModelScope)
    }

    fun onAction(action: StorageAction) {
        when (action) {
            StorageAction.Retry -> loadBreakdown()

            StorageAction.Back -> eventChannel.trySend(StorageEvent.NavigateBack)

            StorageAction.OpenPermissionSettings -> eventChannel.trySend(StorageEvent.OpenUsagePermissionSettings)

            is StorageAction.CopyValue -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    viewModelScope.launch { eventChannel.send(StorageEvent.ShowCopiedFeedback) }
                }
            }
        }
    }

    private fun loadBreakdown() {
        val name = packageName
        if (name == null) {
            isLoading.value = false
            breakdown.value = null
            return
        }
        isLoading.value = true
        viewModelScope.launch {
            breakdown.value = withContext(dispatcherProvider.io()) {
                storageStatsRepository.queryBreakdown(name)
            }
            isLoading.value = false
        }
    }
}
