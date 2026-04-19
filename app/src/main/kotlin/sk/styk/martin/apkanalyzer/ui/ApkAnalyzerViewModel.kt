package sk.styk.martin.apkanalyzer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import javax.inject.Inject

@HiltViewModel
class ApkAnalyzerViewModel
@Inject
constructor(persistenceRepository: PersistenceRepository) : ViewModel() {
    val state: StateFlow<ApkAnalyzerState> =
        persistenceRepository
            .observe(Key.ColorScheme)
            .map { scheme ->
                ApkAnalyzerState.Data(
                    colorAppScheme = scheme,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ApkAnalyzerState.Loading,
            )
}
