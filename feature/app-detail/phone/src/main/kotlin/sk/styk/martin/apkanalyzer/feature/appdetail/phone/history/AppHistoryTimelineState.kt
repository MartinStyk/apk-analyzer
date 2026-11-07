package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySummaryPart
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.ValueChange
import java.time.Instant

internal enum class AppHistoryRowKind {
    FirstRecorded,
    Updated,
    Reinstalled,
    NewDevice,
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
    val isOlderVersion: Boolean = false,
    val isSameVersionBuild: Boolean = false,
    val hasOnlyUncomparableDetails: Boolean = false,
)

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
