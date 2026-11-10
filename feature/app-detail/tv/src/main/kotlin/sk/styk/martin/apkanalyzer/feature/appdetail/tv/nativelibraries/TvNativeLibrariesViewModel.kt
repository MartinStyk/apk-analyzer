package sk.styk.martin.apkanalyzer.feature.appdetail.tv.nativelibraries

import android.os.Build
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
import sk.styk.martin.apkanalyzer.core.apps.packaging.groupedByLibrary
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSection
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSectionRepository

@HiltViewModel(assistedFactory = TvNativeLibrariesViewModel.Factory::class)
internal class TvNativeLibrariesViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val sectionRepository: AppDetailSectionRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appDetailInput: AppDetailInput): TvNativeLibrariesViewModel
    }

    private val mutableState = MutableStateFlow<TvNativeLibrariesState>(TvNativeLibrariesState.Loading)
    val state: StateFlow<TvNativeLibrariesState> = mutableState.asStateFlow()

    init {
        loadLibraries()
    }

    fun onAction(action: TvNativeLibrariesAction) {
        when (action) {
            TvNativeLibrariesAction.Retry -> loadLibraries()
        }
    }

    private fun loadLibraries() {
        mutableState.value = TvNativeLibrariesState.Loading
        viewModelScope.launch(dispatcherProvider.default()) {
            val appName = sectionRepository.detail(appDetailInput).getOrNull()?.info?.applicationName.orEmpty()
            val deviceAbis = Build.SUPPORTED_ABIS.toList()
            mutableState.value = sectionRepository.nativeLibraries(appDetailInput).fold(
                onSuccess = { section ->
                    when (section) {
                        is AppDetailSection.Available -> TvNativeLibrariesState.Loaded(
                            appName = appName,
                            items = section.value.files.groupedByLibrary(deviceAbis.toSet()),
                            deviceAbis = deviceAbis,
                        )

                        AppDetailSection.NotRecorded -> TvNativeLibrariesState.Error
                    }
                },
                onFailure = { TvNativeLibrariesState.Error },
            )
        }
    }
}
