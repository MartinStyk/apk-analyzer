package sk.styk.martin.apkanalyzer.core.apphistory.timeline.model

import java.time.Instant

sealed interface AppHistoryTimeline {
    data class Available(val entries: List<AppHistoryEntry>) : AppHistoryTimeline

    data object Unavailable : AppHistoryTimeline
}

@JvmInline
value class AppHistorySnapshotId(val value: Long)

data class AppHistoryVersion(val versionCode: Long, val versionName: String?)

data class AppHistorySnapshotSummary(
    val id: AppHistorySnapshotId,
    val version: AppHistoryVersion,
    val firstInstallTime: Instant,
    val lastUpdateTime: Instant,
    val capturedAt: Instant,
)

sealed interface AppHistoryEntry {
    val snapshot: AppHistorySnapshotSummary
    val timestamp: Instant

    data class FirstSeen(override val snapshot: AppHistorySnapshotSummary) : AppHistoryEntry {
        override val timestamp: Instant get() = snapshot.lastUpdateTime
    }

    data class Change(
        val kind: Kind,
        override val snapshot: AppHistorySnapshotSummary,
        val previous: AppHistorySnapshotSummary,
        val diff: AppHistoryDiff,
    ) : AppHistoryEntry {
        override val timestamp: Instant
            get() = when (kind) {
                Kind.Updated -> if (snapshot.lastUpdateTime >= previous.lastUpdateTime) snapshot.lastUpdateTime else snapshot.capturedAt
                Kind.Reinstalled -> snapshot.firstInstallTime
                Kind.NewDevice -> snapshot.capturedAt
            }

        enum class Kind { Updated, Reinstalled, NewDevice }
    }
}
