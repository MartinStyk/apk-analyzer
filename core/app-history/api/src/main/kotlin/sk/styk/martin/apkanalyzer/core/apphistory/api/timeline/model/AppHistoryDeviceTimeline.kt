package sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model

import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

sealed interface AppHistoryDeviceTimeline {
    data class Available(val apps: List<AppHistoryAppTimeline>) : AppHistoryDeviceTimeline

    data object Unavailable : AppHistoryDeviceTimeline
}

data class AppHistoryAppTimeline(
    val packageName: PackageName,
    val applicationName: String,
    val entries: List<AppHistoryEntry>,
    val unchangedUpdates: List<Instant>,
    val diffsToLatest: Map<AppHistorySnapshotId, AppHistoryDiff>,
)

val AppHistoryEntry.occurredAt: Instant
    get() = when (this) {
        is AppHistoryEntry.FirstSeen -> snapshot.capturedAt
        is AppHistoryEntry.Change -> timestamp
    }
