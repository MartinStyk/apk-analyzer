package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.SourceFilterCoordinator
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.SourceFilterDraft
import javax.inject.Inject

@HiltViewModel
class SourceFilterViewModel @Inject constructor(
    private val sourceFilterCoordinator: SourceFilterCoordinator,
    installedAppsRepository: InstalledAppsRepository,
) : ViewModel() {

    private val input = sourceFilterCoordinator.consumeInput()
    private val selectedSources = MutableStateFlow(input.selectedSources.toPersistentSet())

    private val eventChannel = Channel<SourceFilterEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val state = combine(installedAppsRepository.apps(), selectedSources) { apps, selected ->
        val options = apps.map { it.source }.distinct()
            .sortedBy { it.ordinal }
            .map { source -> SourceOption(source = source, isSelected = source in selected) }
            .toImmutableList()
        SourceFilterState(options = options)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SourceFilterState())

    fun onAction(action: SourceFilterAction) {
        when (action) {
            is SourceFilterAction.SourceToggled -> selectedSources.update { current ->
                if (action.source in current) (current - action.source).toPersistentSet() else (current + action.source).toPersistentSet()
            }

            SourceFilterAction.Reset -> selectedSources.value = persistentSetOf()

            SourceFilterAction.NavigateBack -> {
                sourceFilterCoordinator.submitResult(SourceFilterDraft(selectedSources = selectedSources.value))
                eventChannel.trySend(SourceFilterEvent.NavigateBack)
            }
        }
    }
}
