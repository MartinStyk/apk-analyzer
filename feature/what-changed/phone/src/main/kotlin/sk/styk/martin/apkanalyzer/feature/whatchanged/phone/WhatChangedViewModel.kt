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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apphistory.api.capture.AppHistoryRecording
import sk.styk.martin.apkanalyzer.core.apphistory.api.capture.AppHistoryRecordingRepository
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
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.access.LockableRow
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.access.WhatChangedAccessPolicy
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.access.toLockableRow
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItem
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItemMapper
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeKind
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
internal class WhatChangedViewModel @Inject constructor(
    private val appHistoryRepository: AppHistoryRepository,
    private val recordingRepository: AppHistoryRecordingRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val persistenceRepository: PersistenceRepository,
    private val visitTracker: WhatChangedVisitTracker,
    private val accessPolicy: WhatChangedAccessPolicy,
    private val itemMapper: AppChangeItemMapper,
    private val clipboardManager: ClipboardManager,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    val state: StateFlow<WhatChangedState>
        field = MutableStateFlow<WhatChangedState>(WhatChangedState.Loading)

    private val eventChannel = Channel<WhatChangedEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val pickedRange = MutableStateFlow<WhatChangedRange?>(null)
    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(action: WhatChangedAction) {
        when (action) {
            WhatChangedAction.Retry -> load()

            WhatChangedAction.OpenFullHistory -> sendEvent(WhatChangedEvent.NavigateToFullHistory)

            WhatChangedAction.OpenSettings -> sendEvent(WhatChangedEvent.NavigateToSettings)

            WhatChangedAction.TurnOnRecording -> viewModelScope.launch { recordingRepository.setRecordingEnabled(true) }

            is WhatChangedAction.SelectRange -> {
                pickedRange.value = action.range
                viewModelScope.launch {
                    if (accessPolicy.hasPro().first()) persistenceRepository.save(Key.WhatChangedRange, action.range.name)
                }
            }

            is WhatChangedAction.OpenChange -> sendEvent(action.item.toNavigationEvent())

            is WhatChangedAction.OpenAppHistory -> sendEvent(WhatChangedEvent.NavigateToAppHistory(action.packageName, action.appName))

            WhatChangedAction.OpenLocked -> (state.value as? WhatChangedState.Loaded)?.let {
                sendEvent(WhatChangedEvent.NavigateToPaywall(it.lockedAppCount))
            }

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
            val initialRange = WhatChangedRange.initial(
                storedName = persistenceRepository[Key.WhatChangedRange],
                hasPro = accessPolicy.hasPro().first(),
            )
            combine(
                appHistoryRepository.deviceTimeline(),
                installedAppsRepository.apps().map { apps -> apps.mapTo(HashSet()) { it.packageName } }.distinctUntilChanged(),
                pickedRange,
                accessPolicy.hasPro(),
                recordingRepository.recording(),
            ) { timeline, installed, picked, hasPro, recording ->
                val range = picked ?: initialRange
                timeline.toState(
                    range = range,
                    previousVisit = previousVisit,
                    installed = installed,
                    hasPro = hasPro,
                    isRecordingOff = recording == AppHistoryRecording.Off,
                )
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
        hasPro: Boolean,
        isRecordingOff: Boolean,
    ): WhatChangedState = when (this) {
        AppHistoryDeviceTimeline.Unavailable -> WhatChangedState.Error

        is AppHistoryDeviceTimeline.Available -> toLoadedState(range, previousVisit, installed, hasPro, isRecordingOff)
            ?: WhatChangedState.Empty(isRecordingOff)
    }

    private fun AppHistoryDeviceTimeline.Available.toLoadedState(
        range: WhatChangedRange,
        previousVisit: Instant?,
        installed: Set<PackageName>,
        hasPro: Boolean,
        isRecordingOff: Boolean,
    ): WhatChangedState.Loaded? {
        val recordingSince = recordingSince ?: return null
        val since = range.since(now = Instant.now(), previousVisit = previousVisit, recordingSince = recordingSince)
        val summary = summarySince(since)
        val access = accessPolicy.accessTo(hasPro)
        val lockedApps = mutableSetOf<PackageName>()
        fun <T> T.gated(
            packageName: PackageName,
            key: String,
            isReadable: Boolean,
        ): LockableRow<T> {
            if (!isReadable) lockedApps += packageName
            return toLockableRow(key, isReadable)
        }
        val needsAttention = summary.needsAttention.mapIndexed { index, attention ->
            val change = attention.change
            val item = itemMapper.map(change.packageName, change.applicationName, change.change, revertedAt = attention.revertedAt)
            item.gated(change.packageName, item.key, access.canReadSectionRow(index, change.change))
        }
        val changedApps = summary.changedApps.mapIndexed { index, app ->
            app.toItem().gated(app.packageName, app.packageName.value, access.canReadSectionRow(index, app))
        }
        val uninstalledApps = apps.filter { it.packageName !in installed }
            .sortedByDescending { app -> app.entries.firstOrNull()?.occurredAt }
            .mapIndexed { index, app -> app.toUninstalledItem().gated(app.packageName, app.packageName.value, access.canReadSectionRow(index)) }
        return WhatChangedState.Loaded(
            range = range,
            since = since,
            updateCount = summary.updateCount,
            newAppCount = summary.newAppCount,
            lastChangeAt = latestChange?.change?.timestamp?.takeIf { summary.changedApps.isEmpty() && it < since },
            needsAttention = needsAttention,
            changedApps = changedApps,
            uninstalledApps = uninstalledApps,
            lockedAppCount = lockedApps.size,
            isRecordingOff = isRecordingOff,
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
