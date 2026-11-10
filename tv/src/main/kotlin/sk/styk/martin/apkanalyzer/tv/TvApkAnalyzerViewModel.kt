package sk.styk.martin.apkanalyzer.tv

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import sk.styk.martin.apkanalyzer.core.common.settings.FingerprintFormat
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import javax.inject.Inject

@HiltViewModel
internal class TvApkAnalyzerViewModel @Inject constructor(persistenceRepository: PersistenceRepository) : ViewModel() {
    val state: StateFlow<TvApkAnalyzerState> = persistenceRepository.observe(Key.HexFingerprintFormat)
        .map { TvApkAnalyzerState.Data(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TvApkAnalyzerState.Loading)
}

@Immutable
internal sealed interface TvApkAnalyzerState {
    data object Loading : TvApkAnalyzerState

    @Immutable
    data class Data(val fingerprintFormat: FingerprintFormat) : TvApkAnalyzerState
}
