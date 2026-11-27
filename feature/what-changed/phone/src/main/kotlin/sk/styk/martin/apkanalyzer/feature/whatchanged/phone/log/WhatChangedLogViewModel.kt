package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryChangeCategory
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryDeviceTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.allEntries
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItem
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItemMapper
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeKind
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
internal class WhatChangedLogViewModel @Inject constructor(
    private val appHistoryRepository: AppHistoryRepository,
    private val itemMapper: AppChangeItemMapper,
    private val clipboardManager: ClipboardManager,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    val state: StateFlow<WhatChangedLogState>
        field = MutableStateFlow<WhatChangedLogState>(WhatChangedLogState.Loading)

    private val eventChannel = Channel<WhatChangedLogEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val selectedFilters = MutableStateFlow<Set<LogFilter>>(emptySet())
    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(action: WhatChangedLogAction) {
        when (action) {
            WhatChangedLogAction.Back -> sendEvent(WhatChangedLogEvent.NavigateBack)

            WhatChangedLogAction.Retry -> load()

            is WhatChangedLogAction.ToggleFilter -> selectedFilters.value = selectedFilters.value.let { selected ->
                if (action.filter in selected) selected - action.filter else selected + action.filter
            }

            is WhatChangedLogAction.OpenItem -> sendEvent(
                when (action.item.kind) {
                    AppChangeKind.FirstRecorded -> WhatChangedLogEvent.NavigateToFullDetail(action.item.historicalInput)
                    is AppChangeKind.Change -> WhatChangedLogEvent.NavigateToChange(action.item.packageName, action.item.snapshotId, action.item.appName)
                },
            )

            is WhatChangedLogAction.CopyItem -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    sendEvent(WhatChangedLogEvent.ShowCopiedFeedback)
                }
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        state.value = WhatChangedLogState.Loading
        loadJob = viewModelScope.launch(dispatcherProvider.default()) {
            combine(appHistoryRepository.deviceTimeline().map { it.toItems() }, selectedFilters) { items, filters ->
                items?.toState(filters) ?: WhatChangedLogState.Error
            }.collect { state.value = it }
        }
    }

    private fun sendEvent(event: WhatChangedLogEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun AppHistoryDeviceTimeline.toItems(): List<AppChangeItem>? = when (this) {
        AppHistoryDeviceTimeline.Unavailable -> null
        is AppHistoryDeviceTimeline.Available -> allEntries.map { itemMapper.map(it.packageName, it.applicationName, it.entry) }
    }
}

private fun List<AppChangeItem>.toState(selectedFilters: Set<LogFilter>): WhatChangedLogState {
    if (isEmpty()) return WhatChangedLogState.Empty
    val available = availableFilters()
    val activeFilters = selectedFilters.intersect(available.toSet())
    val zone = ZoneId.systemDefault()
    return WhatChangedLogState.Loaded(
        months = filter { it.matches(activeFilters) }
            .groupBy { YearMonth.from(it.date.atZone(zone)) }
            .map { (month, items) -> AppChangeMonth(month, items) },
        filters = available,
        selectedFilters = activeFilters,
    )
}

private fun List<AppChangeItem>.availableFilters(): List<LogFilter> {
    val categories = flatMapTo(mutableSetOf()) { it.categories }
    val hasAttention = any { it.needsAttention }
    return buildList {
        if (hasAttention) add(LogFilter.NeedsAttention)
        AppHistoryChangeCategory.entries
            .filter { it != AppHistoryChangeCategory.Version && it in categories }
            .forEach { add(LogFilter.Category(it)) }
    }
}

private fun AppChangeItem.matches(filters: Set<LogFilter>): Boolean = filters.isEmpty() || filters.any { filter ->
    when (filter) {
        LogFilter.NeedsAttention -> needsAttention
        is LogFilter.Category -> filter.category in categories
    }
}
