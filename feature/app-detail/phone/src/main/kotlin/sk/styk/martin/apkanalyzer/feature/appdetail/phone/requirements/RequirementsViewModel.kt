package sk.styk.martin.apkanalyzer.feature.appdetail.phone.requirements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeatures
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeaturesRepository
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.requirements
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.AppDetailDataSource
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.toRecordedVersionFailure

@HiltViewModel(assistedFactory = RequirementsViewModel.Factory::class)
internal class RequirementsViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val dataSource: AppDetailDataSource,
    private val deviceFeaturesRepository: DeviceFeaturesRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val clipboardManager: ClipboardManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(target: AppDetailInput): RequirementsViewModel
    }

    val state: StateFlow<RequirementsState>
        field = MutableStateFlow<RequirementsState>(RequirementsState.Loading)

    private val _events = Channel<RequirementsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadRequirements()
    }

    fun onAction(action: RequirementsAction) {
        when (action) {
            is RequirementsAction.Retry -> loadRequirements()

            is RequirementsAction.CopyValue -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    viewModelScope.launch { _events.send(RequirementsEvent.ShowCopiedFeedback) }
                }
            }
        }
    }

    private fun loadRequirements() {
        state.value = RequirementsState.Loading
        viewModelScope.launch {
            val isHistorical = appDetailInput is AppDetailInput.Historical
            val deviceFeatures = if (isHistorical) DeviceFeatures.Unknown else deviceFeaturesRepository.deviceFeatures()
            state.value = withContext(dispatcherProvider.default()) {
                dataSource.detail(appDetailInput)
            }.fold(
                onSuccess = { detail ->
                    detail.requirements(deviceFeatures).let { RequirementsState.Loaded(it.sections, it.missingRequiredCount, isHistorical) }
                },
                onFailure = { failure -> failure.toRecordedVersionFailure()?.let { RequirementsState.RecordedVersionError(it) } ?: RequirementsState.Error },
            )
        }
    }
}
