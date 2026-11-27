package sk.styk.martin.apkanalyzer.feature.whatchanged.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryAppTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryChangedApp
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryDeviceTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.changeCount
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.hasCertificateChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.latestChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.needsAttention
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.occurredAt
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.recordingSince
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.summaryParts
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.summarySince
import sk.styk.martin.apkanalyzer.core.apps.api.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItem
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItemMapper
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeKind
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
internal class WhatChangedViewModel @Inject constructor(
    private val appHistoryRepository: AppHistoryRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val persistenceRepository: PersistenceRepository,
    private val visitTracker: WhatChangedVisitTracker,
    private val itemMapper: AppChangeItemMapper,
    private val clipboardManager: ClipboardManager,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    val state: StateFlow<WhatChangedState>
        field = MutableStateFlow<WhatChangedState>(WhatChangedState.Loading)

    private val eventChannel = Channel<WhatChangedEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val selectedRange = MutableStateFlow<WhatChangedRange?>(null)
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            selectedRange.value = WhatChangedRange.fromStored(persistenceRepository.get(Key.WhatChangedRange))
        }
        load()
    }

    fun onAction(action: WhatChangedAction) {
        when (action) {
            WhatChangedAction.Retry -> load()

            WhatChangedAction.OpenFullHistory -> sendEvent(WhatChangedEvent.NavigateToFullHistory)

            is WhatChangedAction.SelectRange -> {
                selectedRange.value = action.range
                viewModelScope.launch { persistenceRepository.save(Key.WhatChangedRange, action.range.name) }
            }

            is WhatChangedAction.OpenChange -> sendEvent(action.item.toNavigationEvent())

            is WhatChangedAction.OpenAppHistory -> sendEvent(WhatChangedEvent.NavigateToAppHistory(action.packageName, action.appName))

            is WhatChangedAction.CopyItem -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    sendEvent(WhatChangedEvent.ShowCopiedFeedback)
                }
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        state.value = WhatChangedState.Loading
        loadJob = viewModelScope.launch(dispatcherProvider.default()) {
            val previousVisit = visitTracker.previousVisit()
            combine(
                appHistoryRepository.deviceTimeline(),
                installedAppsRepository.apps().map { apps -> apps.mapTo(HashSet()) { it.packageName } }.distinctUntilChanged(),
                selectedRange.filterNotNull(),
            ) { timeline, installed, range ->
                timeline.toState(range = range, previousVisit = previousVisit, installed = installed)
            }.collect { state.value = it }
        }
    }

    private fun sendEvent(event: WhatChangedEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun AppHistoryDeviceTimeline.toState(
        range: WhatChangedRange,
        previousVisit: Instant?,
        installed: Set<PackageName>,
    ): WhatChangedState = when (this) {
        AppHistoryDeviceTimeline.Unavailable -> WhatChangedState.Error
        is AppHistoryDeviceTimeline.Available -> toLoadedState(range, previousVisit, installed) ?: WhatChangedState.Empty
    }

    private fun AppHistoryDeviceTimeline.Available.toLoadedState(
        range: WhatChangedRange,
        previousVisit: Instant?,
        installed: Set<PackageName>,
    ): WhatChangedState.Loaded? {
        val recordingSince = recordingSince ?: return null
        val since = range.since(now = Instant.now(), previousVisit = previousVisit, recordingSince = recordingSince)
        val summary = summarySince(since)
        return WhatChangedState.Loaded(
            range = range,
            since = since,
            updateCount = summary.updateCount,
            newAppCount = summary.newAppCount,
            lastChangeAt = latestChange?.change?.timestamp?.takeIf { summary.changedApps.isEmpty() && it < since },
            needsAttention = summary.needsAttention.map { item ->
                itemMapper.map(item.change.packageName, item.change.applicationName, item.change.change, revertedAt = item.revertedAt)
            },
            changedApps = summary.changedApps.map { it.toItem() },
            uninstalledApps = apps.filter { it.packageName !in installed }
                .sortedByDescending { app -> app.entries.firstOrNull()?.occurredAt }
                .map { it.toUninstalledItem() },
        )
    }

    private fun AppHistoryChangedApp.toItem() = ChangedAppItem(
        packageName = packageName.value,
        appName = applicationName,
        updateCount = updateCount,
        isNew = isNew,
        netChange = when (val diff = netChange) {
            null -> NetChange.NotCompared

            else -> {
                val parts = diff.summaryParts(itemMapper::permissionLabel).sortedByDescending { it.needsAttention }
                if (parts.isEmpty() && !diff.hasCertificateChange) NetChange.None else NetChange.Changed(parts, diff.hasCertificateChange)
            }
        },
    )

    private fun AppHistoryAppTimeline.toUninstalledItem() = UninstalledAppItem(
        packageName = packageName.value,
        appName = applicationName,
        changeCount = entries.changeCount,
    )
}

private fun AppChangeItem.toNavigationEvent(): WhatChangedEvent = when (kind) {
    AppChangeKind.FirstRecorded -> WhatChangedEvent.NavigateToFullDetail(historicalInput)
    is AppChangeKind.Change -> WhatChangedEvent.NavigateToChange(packageName, snapshotId, appName)
}
