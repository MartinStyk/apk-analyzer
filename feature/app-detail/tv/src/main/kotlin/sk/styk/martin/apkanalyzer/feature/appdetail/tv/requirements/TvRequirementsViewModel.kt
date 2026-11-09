package sk.styk.martin.apkanalyzer.feature.appdetail.tv.requirements

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

    private val mutableState = MutableStateFlow<TvRequirementsState>(TvRequirementsState.Loading)
    val state: StateFlow<TvRequirementsState> = mutableState.asStateFlow()

    init {
        loadRequirements()
    }

    fun onAction(action: TvRequirementsAction) {
        when (action) {
            TvRequirementsAction.Retry -> loadRequirements()
        }
    }

    private fun loadRequirements() {
        mutableState.value = TvRequirementsState.Loading
        viewModelScope.launch(dispatcherProvider.default()) {
            val deviceFeatures = deviceFeaturesRepository.deviceFeatures()
            mutableState.value = sectionRepository.detail(appDetailInput).fold(
                onSuccess = { TvRequirementsState.Loaded(appName = it.info.applicationName, requirements = it.requirements(deviceFeatures)) },
                onFailure = { TvRequirementsState.Error },
            )
        }
    }
}
