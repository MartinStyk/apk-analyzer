package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.timeline

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySummaryPart
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.HistoryVersionTag
import java.time.Instant

@Immutable
internal sealed interface AppHistoryRowKind {
    data object FirstRecorded : AppHistoryRowKind

    data class Change(val kind: AppHistoryEntry.Change.Kind) : AppHistoryRowKind
}

@Immutable
internal data class AppHistoryRow(
    val snapshotId: Long,
    val capturedAt: Instant,
    val date: Instant,
    val kind: AppHistoryRowKind,
    val version: AppHistoryVersion,
    val versionChange: ValueChange<AppHistoryVersion>? = null,
    val summary: List<AppHistorySummaryPart> = emptyList(),
    val hasCertificateChange: Boolean = false,
    val isQuiet: Boolean = false,
    val versionTag: HistoryVersionTag? = null,
    val hasOnlyUncomparableDetails: Boolean = false,
    val showsTime: Boolean = false,
) {
    val changeKind: AppHistoryEntry.Change.Kind?
        get() = (kind as? AppHistoryRowKind.Change)?.kind
}

@Immutable
internal sealed interface AppHistoryTimelineState {
    data object Loading : AppHistoryTimelineState

    data object Error : AppHistoryTimelineState

    data object Empty : AppHistoryTimelineState

    @Immutable
    data class Loaded(
        val appName: String,
        val recordingSince: Instant,
        val changeCount: Int,
        val runs: List<List<AppHistoryRow>>,
    ) : AppHistoryTimelineState
}
