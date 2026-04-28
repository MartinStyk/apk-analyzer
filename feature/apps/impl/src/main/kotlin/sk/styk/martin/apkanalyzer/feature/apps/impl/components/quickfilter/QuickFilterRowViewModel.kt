package sk.styk.martin.apkanalyzer.feature.apps.impl.components.quickfilter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterRepository
import javax.inject.Inject

@HiltViewModel
class QuickFilterRowViewModel @Inject constructor(private val appFilterRepository: AppFilterRepository) : ViewModel() {

    private val eventChannel = Channel<QuickFilterRowEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val state = combine(
        appFilterRepository.activeQuickFilters,
        appFilterRepository.filter,
    ) { activeFilters, filterState ->
        QuickFilterRowState(
            activeQuickFilters = activeFilters,
            isDeepFilterActive = filterState.isActive,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickFilterRowState())

    fun onAction(action: QuickFilterRowAction) {
        when (action) {
            is QuickFilterRowAction.QuickFilterToggle -> appFilterRepository.toggleQuickFilter(action.filter)
            is QuickFilterRowAction.FilterClick -> eventChannel.trySend(QuickFilterRowEvent.NavigateToFilter)
        }
    }
}
