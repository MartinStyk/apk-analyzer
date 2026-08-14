package sk.styk.martin.apkanalyzer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.common.review.ReviewEligibilityTracker
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import javax.inject.Inject

@HiltViewModel
class ApkAnalyzerViewModel @Inject constructor(persistenceRepository: PersistenceRepository, private val reviewEligibilityTracker: ReviewEligibilityTracker) :
    ViewModel() {
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

    private val eventChannel = Channel<ApkAnalyzerEvent>(Channel.BUFFERED)
    internal val events = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            reviewEligibilityTracker.reviewPromptRequests.collect {
                eventChannel.send(ApkAnalyzerEvent.RequestReview)
            }
        }
    }
}
