package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.changeCount
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.hasCertificateChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.hasOnlyUncomparableDetails
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.isOlderVersion
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.isQuiet
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.isSameVersionBuild
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.recordingSince
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.summaryParts
import sk.styk.martin.apkanalyzer.core.apppermissions.PermissionLabelProvider
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.AppDetailAnalytics
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.AppDetailAnalyticsEvent

private const val FEW_CHANGES_LIMIT = 5

@HiltViewModel(assistedFactory = AppHistoryTimelineViewModel.Factory::class)
internal class AppHistoryTimelineViewModel @AssistedInject constructor(
    @Assisted private val packageName: String,
    appHistoryRepository: AppHistoryRepository,
    private val permissionLabelProvider: PermissionLabelProvider,
    private val clipboardManager: ClipboardManager,
    private val analytics: AppDetailAnalytics,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(packageName: String): AppHistoryTimelineViewModel
    }

    private var isOpenTracked = false

    private val timeline = RetryableFlow<AppHistoryTimelineState>(loading = AppHistoryTimelineState.Loading) {
        appHistoryRepository.timeline(PackageName(packageName))
            .onEach(::trackOpened)
            .map { it.toState() }
    }

    val state: StateFlow<AppHistoryTimelineState> = timeline.flow
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppHistoryTimelineState.Loading)

    private val eventChannel = Channel<AppHistoryTimelineEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: AppHistoryTimelineAction) {
        when (action) {
            AppHistoryTimelineAction.Back -> sendEvent(AppHistoryTimelineEvent.NavigateBack)

            AppHistoryTimelineAction.Retry -> timeline.retry()

            is AppHistoryTimelineAction.OpenRow -> openRow(action.row, action.appName)

            is AppHistoryTimelineAction.CopyRow -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    sendEvent(AppHistoryTimelineEvent.ShowCopiedFeedback)
                }
            }
        }
    }

    private fun openRow(row: AppHistoryRow, appName: String) {
        if (row.kind == AppHistoryRowKind.FirstRecorded) {
            analytics.track(AppDetailAnalyticsEvent.HistoryFullDetailOpened)
            sendEvent(
                AppHistoryTimelineEvent.NavigateToFullDetail(
                    AppDetailInput.Historical(
                        packageName = packageName,
                        snapshotId = row.snapshotId,
                        capturedAtEpochMillis = row.capturedAt.toEpochMilli(),
                    ),
                ),
            )
        } else {
            sendEvent(AppHistoryTimelineEvent.NavigateToChange(row.snapshotId, appName))
        }
    }

    private fun sendEvent(event: AppHistoryTimelineEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun trackOpened(timeline: AppHistoryTimeline) {
        if (isOpenTracked || timeline !is AppHistoryTimeline.Available) return
        isOpenTracked = true
        val changeCount = timeline.entries.changeCount
        analytics.track(
            AppDetailAnalyticsEvent.HistoryTimelineOpened(
                when {
                    timeline.entries.isEmpty() -> AppDetailAnalyticsEvent.HistoryBucket.Empty
                    changeCount == 0 -> AppDetailAnalyticsEvent.HistoryBucket.FirstOnly
                    changeCount <= FEW_CHANGES_LIMIT -> AppDetailAnalyticsEvent.HistoryBucket.OneToFive
                    else -> AppDetailAnalyticsEvent.HistoryBucket.SixOrMore
                },
            ),
        )
    }

    private fun AppHistoryTimeline.toState(): AppHistoryTimelineState = when (this) {
        AppHistoryTimeline.Unavailable -> AppHistoryTimelineState.Error
        is AppHistoryTimeline.Available -> entries.toLoadedState() ?: AppHistoryTimelineState.Empty
    }

    private fun List<AppHistoryEntry>.toLoadedState(): AppHistoryTimelineState.Loaded? {
        val since = recordingSince ?: return null
        return AppHistoryTimelineState.Loaded(
            appName = first().snapshot.applicationName,
            recordingSince = since,
            changeCount = changeCount,
            runs = map { it.toRow() }.splitIntoRuns(),
        )
    }

    private fun AppHistoryEntry.toRow(): AppHistoryRow = when (this) {
        is AppHistoryEntry.FirstSeen -> AppHistoryRow(
            snapshotId = snapshot.id.value,
            capturedAt = snapshot.capturedAt,
            date = snapshot.capturedAt,
            kind = AppHistoryRowKind.FirstRecorded,
            version = snapshot.version,
        )

        is AppHistoryEntry.Change -> AppHistoryRow(
            snapshotId = snapshot.id.value,
            capturedAt = snapshot.capturedAt,
            date = timestamp,
            kind = when (kind) {
                AppHistoryEntry.Change.Kind.Updated -> AppHistoryRowKind.Updated
                AppHistoryEntry.Change.Kind.Reinstalled -> AppHistoryRowKind.Reinstalled
                AppHistoryEntry.Change.Kind.NewDevice -> AppHistoryRowKind.NewDevice
            },
            version = snapshot.version,
            versionChange = diff.version,
            summary = summaryParts(permissionLabelProvider::getLabel),
            hasCertificateChange = diff.hasCertificateChange,
            isQuiet = isQuiet,
            isOlderVersion = isOlderVersion,
            isSameVersionBuild = isSameVersionBuild,
            hasOnlyUncomparableDetails = hasOnlyUncomparableDetails,
        )
    }
}

private fun List<AppHistoryRow>.splitIntoRuns(): List<List<AppHistoryRow>> {
    val runs = mutableListOf(mutableListOf<AppHistoryRow>())
    forEachIndexed { index, row ->
        runs.last() += row
        val endsRun = row.kind == AppHistoryRowKind.Reinstalled || row.kind == AppHistoryRowKind.NewDevice
        if (endsRun && index != lastIndex) runs += mutableListOf<AppHistoryRow>()
    }
    return runs
}
