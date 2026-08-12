package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.aisummary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.aiinsights.ai.AiAvailability
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiDescriptionRepository
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

@HiltViewModel(assistedFactory = AiSummaryViewModel.Factory::class)
internal class AiSummaryViewModel @AssistedInject constructor(
    @Assisted private val reference: AppReference,
    private val appAiDescriptionRepository: AppAiDescriptionRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(reference: AppReference): AiSummaryViewModel
    }

    val state: StateFlow<AiSummaryState>
        field = MutableStateFlow<AiSummaryState>(AiSummaryState.Hidden)

    init {
        viewModelScope.launch {
            appAiDescriptionRepository.availability.collect { availability ->
                when (availability) {
                    AiAvailability.Available -> loadDescription()
                    AiAvailability.Downloadable -> state.value = AiSummaryState.Downloadable
                    AiAvailability.Downloading -> state.value = AiSummaryState.Downloading
                    AiAvailability.Unavailable -> state.value = AiSummaryState.Hidden
                }
            }
        }
    }

    fun onAction(action: AiSummaryAction) {
        when (action) {
            AiSummaryAction.DownloadModel -> appAiDescriptionRepository.downloadModel()
        }
    }

    private suspend fun loadDescription() {
        state.value = AiSummaryState.Loading
        val description = appAiDescriptionRepository.getDescription(reference)
        state.value = description?.let { AiSummaryState.Loaded(it) } ?: AiSummaryState.Hidden
    }
}
