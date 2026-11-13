package sk.styk.martin.apkanalyzer.feature.appdetail.tv.requirements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeaturesRepository
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.requirements
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSectionRepository

@HiltViewModel(assistedFactory = TvRequirementsViewModel.Factory::class)
internal class TvRequirementsViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val sectionRepository: AppDetailSectionRepository,
    private val deviceFeaturesRepository: DeviceFeaturesRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appDetailInput: AppDetailInput): TvRequirementsViewModel
    }

    val state: StateFlow<TvRequirementsState>
        field = MutableStateFlow<TvRequirementsState>(TvRequirementsState.Loading)

    init {
        loadRequirements()
    }

    fun onAction(action: TvRequirementsAction) {
        when (action) {
            TvRequirementsAction.Retry -> loadRequirements()
        }
    }

    private fun loadRequirements() {
        state.value = TvRequirementsState.Loading
        viewModelScope.launch(dispatcherProvider.default()) {
            val deviceFeatures = deviceFeaturesRepository.deviceFeatures()
            state.value = sectionRepository.detail(appDetailInput).fold(
                onSuccess = { TvRequirementsState.Loaded(appName = it.info.applicationName, requirements = it.requirements(deviceFeatures)) },
                onFailure = { TvRequirementsState.Error },
            )
        }
    }
}
