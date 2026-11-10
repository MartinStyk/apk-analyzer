package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.changeCount
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.hasCertificateChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.hasOnlyUncomparableDetails
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.isQuiet
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.recordingSince
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.summaryParts
import sk.styk.martin.apkanalyzer.core.apppermissions.PermissionLabelProvider
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.analytics.AppDetailAnalytics
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.analytics.AppDetailAnalyticsEvent
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.versionTag
import java.time.ZoneId

private const val FEW_CHANGES_LIMIT = 5

@HiltViewModel(assistedFactory = AppHistoryTimelineViewModel.Factory::class)
internal class AppHistoryTimelineViewModel @AssistedInject constructor(
    @Assisted private val packageName: String,
    private val appHistoryRepository: AppHistoryRepository,
    private val permissionLabelProvider: PermissionLabelProvider,
    private val clipboardManager: ClipboardManager,
    private val analytics: AppDetailAnalytics,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(packageName: String): AppHistoryTimelineViewModel
    }

    private var isOpenTracked = false
    private var loadJob: Job? = null

    val state: StateFlow<AppHistoryTimelineState>
        field = MutableStateFlow<AppHistoryTimelineState>(AppHistoryTimelineState.Loading)

    private val eventChannel = Channel<AppHistoryTimelineEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: AppHistoryTimelineAction) {
        when (action) {
            AppHistoryTimelineAction.Back -> sendEvent(AppHistoryTimelineEvent.NavigateBack)

            AppHistoryTimelineAction.Retry -> load()

            is AppHistoryTimelineAction.OpenRow -> openRow(action.row)

            is AppHistoryTimelineAction.CopyRow -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    sendEvent(AppHistoryTimelineEvent.ShowCopiedFeedback)
                }
            }
        }
    }

    private fun openRow(row: AppHistoryRow) {
        val appName = (state.value as? AppHistoryTimelineState.Loaded)?.appName ?: return
        if (row.kind == AppHistoryRowKind.FirstRecorded) {
            sendEvent(
                AppHistoryTimelineEvent.NavigateToFullDetail(
                    AppDetailInput.Historical(
                        packageName = packageName,
                        snapshotId = row.snapshotId,
                        capturedAt = row.capturedAt,
                    ),
                ),
            )
        } else {
            sendEvent(AppHistoryTimelineEvent.NavigateToChange(row.snapshotId, appName))
        }
    }

    private fun load() {
        loadJob?.cancel()
        state.value = AppHistoryTimelineState.Loading
        loadJob = viewModelScope.launch(dispatcherProvider.default()) {
            appHistoryRepository.timeline(PackageName(packageName)).collect { timeline ->
                trackOpened(timeline)
                state.value = timeline.toState()
            }
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
                bucket = when {
                    timeline.entries.isEmpty() -> AppDetailAnalyticsEvent.HistoryBucket.Empty
                    changeCount == 0 -> AppDetailAnalyticsEvent.HistoryBucket.FirstOnly
                    changeCount <= FEW_CHANGES_LIMIT -> AppDetailAnalyticsEvent.HistoryBucket.OneToFive
                    else -> AppDetailAnalyticsEvent.HistoryBucket.SixOrMore
                },
                categories = timeline.entries.filterIsInstance<AppHistoryEntry.Change>().flatMapTo(mutableSetOf()) { it.diff.changedCategories },
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
            runs = map { it.toRow() }.withTimeOnSharedDays().splitIntoRuns(),
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
            kind = AppHistoryRowKind.Change(kind),
            version = snapshot.version,
            versionChange = diff.version,
            summary = summaryParts(permissionLabelProvider::getLabel),
            hasCertificateChange = diff.hasCertificateChange,
            isQuiet = isQuiet,
            versionTag = versionTag,
            hasOnlyUncomparableDetails = hasOnlyUncomparableDetails,
        )
    }
}

private fun List<AppHistoryRow>.withTimeOnSharedDays(): List<AppHistoryRow> {
    val zone = ZoneId.systemDefault()
    val rowsPerDay = groupingBy { it.date.atZone(zone).toLocalDate() }.eachCount()
    return map { it.copy(showsTime = rowsPerDay.getValue(it.date.atZone(zone).toLocalDate()) > 1) }
}

private fun List<AppHistoryRow>.splitIntoRuns(): List<List<AppHistoryRow>> {
    val runs = mutableListOf(mutableListOf<AppHistoryRow>())
    forEachIndexed { index, row ->
        runs.last() += row
        val endsRun = row.changeKind == AppHistoryEntry.Change.Kind.Reinstalled || row.changeKind == AppHistoryEntry.Change.Kind.NewDevice
        if (endsRun && index != lastIndex) runs += mutableListOf<AppHistoryRow>()
    }
    return runs
}
