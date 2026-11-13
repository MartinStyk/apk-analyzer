package sk.styk.martin.apkanalyzer.feature.appdetail.tv.splitapks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSectionRepository

@HiltViewModel(assistedFactory = TvSplitApksViewModel.Factory::class)
internal class TvSplitApksViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val sectionRepository: AppDetailSectionRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appDetailInput: AppDetailInput): TvSplitApksViewModel
    }

    val state: StateFlow<TvSplitApksState>
        field = MutableStateFlow<TvSplitApksState>(TvSplitApksState.Loading)

    init {
        loadSplits()
    }

    fun onAction(action: TvSplitApksAction) {
        when (action) {
            TvSplitApksAction.Retry -> loadSplits()
        }
    }

    private fun loadSplits() {
        state.value = TvSplitApksState.Loading
        viewModelScope.launch(dispatcherProvider.default()) {
            state.value = sectionRepository.detail(appDetailInput).fold(
                onSuccess = { detail ->
                    TvSplitApksState.Loaded(
                        appName = detail.info.applicationName,
                        splits = detail.info.installedSplits.sortedWith(compareBy({ it.kind }, { it.qualifier })),
                    )
                },
                onFailure = { TvSplitApksState.Error },
            )
        }
    }
}
