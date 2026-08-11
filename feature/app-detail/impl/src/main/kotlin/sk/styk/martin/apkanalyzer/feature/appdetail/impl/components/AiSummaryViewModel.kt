package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiDescription
import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiDescriptionRepository
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

@HiltViewModel(assistedFactory = AiSummaryViewModel.Factory::class)
internal class AiSummaryViewModel @AssistedInject constructor(
    @Assisted private val reference: AppReference,
    private val appAiDescriptionRepository: AppAiDescriptionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AppAiDescription?>(null)
    val state: StateFlow<AppAiDescription?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = appAiDescriptionRepository.getDescription(reference)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(reference: AppReference): AiSummaryViewModel
    }
}
