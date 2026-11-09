package sk.styk.martin.apkanalyzer.feature.appdetail.tv.manifest

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
import sk.styk.martin.apkanalyzer.core.apps.manifest.ManifestParser
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.liveReference

@HiltViewModel(assistedFactory = TvManifestViewModel.Factory::class)
internal class TvManifestViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val manifestParser: ManifestParser,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appDetailInput: AppDetailInput): TvManifestViewModel
    }

    private val mutableState = MutableStateFlow<TvManifestState>(TvManifestState.Loading)
    val state: StateFlow<TvManifestState> = mutableState.asStateFlow()

    init {
        loadManifest()
    }

    fun onAction(action: TvManifestAction) {
        when (action) {
            TvManifestAction.Retry -> loadManifest()
        }
    }

    private fun loadManifest() {
        mutableState.value = TvManifestState.Loading
        viewModelScope.launch(dispatcherProvider.default()) {
            mutableState.value = manifestParser.manifest(checkNotNull(appDetailInput.liveReference)).fold(
                onSuccess = { manifest ->
                    TvManifestState.Loaded(
                        lines = manifest.xml.lines(),
                        additionalInstalledSplits = manifest.additionalInstalledSplits,
                    )
                },
                onFailure = { TvManifestState.Error },
            )
        }
    }
}
