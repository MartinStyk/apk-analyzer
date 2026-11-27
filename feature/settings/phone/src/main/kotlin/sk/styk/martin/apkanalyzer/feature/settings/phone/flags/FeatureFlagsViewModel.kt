package sk.styk.martin.apkanalyzer.feature.settings.phone.flags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagOverridesRepository
import java.util.Optional
import javax.inject.Inject

private const val TAG = "FeatureFlagsViewModel"

@HiltViewModel
internal class FeatureFlagsViewModel @Inject constructor(private val overridesRepository: Optional<FeatureFlagOverridesRepository>) : ViewModel() {
    val state: StateFlow<FeatureFlagsState>
        field = MutableStateFlow<FeatureFlagsState>(FeatureFlagsState.Loading)

    private val eventChannel = Channel<FeatureFlagsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var observation: Job? = null
    private var isSaving = false

    init {
        observe()
    }

    fun onAction(action: FeatureFlagsAction) {
        when (action) {
            FeatureFlagsAction.NavigateBack -> eventChannel.trySend(FeatureFlagsEvent.NavigateBack)
            FeatureFlagsAction.Retry -> observe()
            FeatureFlagsAction.ResetAll -> save { overridesRepository.get().resetAll() }
            is FeatureFlagsAction.SetOverride -> save { overridesRepository.get().set(action.flag, action.override) }
        }
    }

    private fun observe() {
        if (!overridesRepository.isPresent) {
            Logger.e(TAG, IllegalStateException("No debug override backend"), "Feature flag editor is unavailable")
            state.value = FeatureFlagsState.Error
            return
        }
        observation?.cancel()
        state.value = FeatureFlagsState.Loading
        observation = viewModelScope.launch {
            overridesRepository.get().observe().collect { result ->
                state.value = result.fold(
                    onSuccess = { FeatureFlagsState.Content(it, isSaving) },
                    onFailure = { FeatureFlagsState.Error },
                )
            }
        }
    }

    private fun save(operation: suspend () -> Result<Unit>) {
        val content = state.value as? FeatureFlagsState.Content ?: return
        if (isSaving) return
        isSaving = true
        state.value = content.copy(isSaving = true)
        viewModelScope.launch {
            try {
                operation().onFailure {
                    Logger.e(TAG, it, "Could not save feature flag overrides")
                    eventChannel.send(FeatureFlagsEvent.SaveFailed)
                }
            } finally {
                isSaving = false
                (state.value as? FeatureFlagsState.Content)?.let { state.value = it.copy(isSaving = false) }
            }
        }
    }
}
