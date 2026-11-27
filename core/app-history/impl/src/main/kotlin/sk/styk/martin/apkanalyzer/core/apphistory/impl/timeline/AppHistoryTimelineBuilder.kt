package sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline

import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryAppTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshotId
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshotSummary
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistorySnapshotHeader
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.StoredAppHistoryRecord
import javax.inject.Inject

internal class AppHistoryTimelineBuilder @Inject constructor(private val differ: AppHistoryDiffer) {

    fun entries(records: List<StoredAppHistoryRecord>): List<AppHistoryEntry> {
        val first = records.firstOrNull() ?: return emptyList()
        return changes(records).filterNot { it.isUpdateWithoutVisibleChange }.asReversed() + AppHistoryEntry.FirstSeen(first.toSummary())
    }

    fun appTimeline(records: List<StoredAppHistoryRecord>): AppHistoryAppTimeline? {
        val first = records.firstOrNull() ?: return null
        val latest = records.last()
        val (unchangedUpdates, visibleChanges) = changes(records).partition { it.isUpdateWithoutVisibleChange }
        return AppHistoryAppTimeline(
            packageName = latest.record.packageName,
            applicationName = latest.record.detail.info.applicationName,
            entries = visibleChanges.asReversed() + AppHistoryEntry.FirstSeen(first.toSummary()),
            unchangedUpdates = unchangedUpdates.map { it.timestamp },
            diffsToLatest = records.dropLast(1).associate { AppHistorySnapshotId(it.id) to differ(before = it.record, after = latest.record) },
        )
    }

    fun firstSeenOnly(header: AppHistorySnapshotHeader) = AppHistoryAppTimeline(
        packageName = header.packageName,
        applicationName = header.applicationName,
        entries = listOf(AppHistoryEntry.FirstSeen(header.toSummary())),
        unchangedUpdates = emptyList(),
        diffsToLatest = emptyMap(),
    )

    private fun changes(records: List<StoredAppHistoryRecord>): List<AppHistoryEntry.Change> = records.zipWithNext { previous, current ->
        val kind = when {
            current isOnNewDeviceAfter previous -> AppHistoryEntry.Change.Kind.NewDevice
            current isReinstallOf previous -> AppHistoryEntry.Change.Kind.Reinstalled
            else -> AppHistoryEntry.Change.Kind.Updated
        }
        AppHistoryEntry.Change(
            kind = kind,
            snapshot = current.toSummary(),
            previous = previous.toSummary(),
            diff = differ(before = previous.record, after = current.record),
        )
    }
}

private val AppHistoryEntry.Change.isUpdateWithoutVisibleChange: Boolean
    get() = kind == AppHistoryEntry.Change.Kind.Updated && diff.changedCategories.isEmpty() && diff.uncomparableCategories.isEmpty()

private infix fun StoredAppHistoryRecord.isOnNewDeviceAfter(previous: StoredAppHistoryRecord): Boolean =
    record.captureKey.deviceId != previous.record.captureKey.deviceId

private infix fun StoredAppHistoryRecord.isReinstallOf(previous: StoredAppHistoryRecord): Boolean =
    record.captureKey.firstInstallTime != previous.record.captureKey.firstInstallTime

private fun StoredAppHistoryRecord.toSummary() = AppHistorySnapshotSummary(
    id = AppHistorySnapshotId(id),
    applicationName = record.detail.info.applicationName,
    version = record.detail.info.version,
    firstInstallTime = record.captureKey.firstInstallTime,
    lastUpdateTime = record.captureKey.lastUpdateTime,
    capturedAt = record.capturedAt,
)

private fun AppHistorySnapshotHeader.toSummary() = AppHistorySnapshotSummary(
    id = AppHistorySnapshotId(id),
    applicationName = applicationName,
    version = AppHistoryVersion(versionCode = versionCode, versionName = versionName),
    firstInstallTime = captureKey.firstInstallTime,
    lastUpdateTime = captureKey.lastUpdateTime,
    capturedAt = capturedAt,
)
