package sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline

import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryAppTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryDiff
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySection
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshotId
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshotSummary
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryDevice
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistorySnapshotHeader
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.CapturedSection
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.StoredAppHistoryRecord
import javax.inject.Inject

internal class AppHistoryTimelineBuilder @Inject constructor(
    private val differ: AppHistoryDiffer,
    private val recordedOnResolver: AppHistoryRecordedOnResolver,
) {

    fun entries(records: List<StoredAppHistoryRecord>, devices: Map<String, AppHistoryDevice>): List<AppHistoryEntry> {
        val observed = records.withoutRepeatedObservations()
        val first = observed.firstOrNull() ?: return emptyList()
        return changes(observed, devices).filterNot { it.isUpdateWithoutVisibleChange }.asReversed() +
            AppHistoryEntry.FirstSeen(first.toSummary(devices))
    }

    fun appTimeline(records: List<StoredAppHistoryRecord>, devices: Map<String, AppHistoryDevice>): AppHistoryAppTimeline? {
        val observed = records.withoutRepeatedObservations()
        val first = observed.firstOrNull() ?: return null
        val latest = observed.last()
        val (unchangedUpdates, visibleChanges) = changes(observed, devices).partition { it.isUpdateWithoutVisibleChange }
        return AppHistoryAppTimeline(
            packageName = latest.record.packageName,
            applicationName = latest.record.detail.info.applicationName,
            entries = visibleChanges.asReversed() + AppHistoryEntry.FirstSeen(first.toSummary(devices)),
            unchangedUpdates = unchangedUpdates.map { it.timestamp },
            diffsToLatest = observed.dropLast(1).associate { AppHistorySnapshotId(it.id) to differ(before = it.record, after = latest.record) },
        )
    }

    fun firstSeenOnly(header: AppHistorySnapshotHeader, devices: Map<String, AppHistoryDevice>) = AppHistoryAppTimeline(
        packageName = header.packageName,
        applicationName = header.applicationName,
        entries = listOf(AppHistoryEntry.FirstSeen(header.toSummary(devices))),
        unchangedUpdates = emptyList(),
        diffsToLatest = emptyMap(),
    )

    fun snapshot(stored: StoredAppHistoryRecord, devices: Map<String, AppHistoryDevice>) = AppHistorySnapshot(
        id = AppHistorySnapshotId(stored.id),
        capturedAt = stored.record.capturedAt,
        recordedOn = recordedOnResolver.resolve(stored.record.captureKey.deviceId, devices),
        detail = stored.record.detail,
        nativeLibraries = stored.record.nativeLibraries.toSection(),
        signingSchemes = stored.record.signingScheme.toSection(),
    )

    private fun List<StoredAppHistoryRecord>.withoutRepeatedObservations(): List<StoredAppHistoryRecord> {
        val kept = mutableListOf<StoredAppHistoryRecord>()
        forEach { record ->
            val previous = kept.lastOrNull()
            val repeatsPrevious = previous != null &&
                record.record.captureKey == previous.record.captureKey &&
                !differ(before = previous.record, after = record.record).isVisible
            if (!repeatsPrevious) kept += record
        }
        return kept
    }

    private fun changes(records: List<StoredAppHistoryRecord>, devices: Map<String, AppHistoryDevice>): List<AppHistoryEntry.Change> =
        records.zipWithNext { previous, current ->
            val kind = when {
                current isOnNewDeviceAfter previous -> AppHistoryEntry.Change.Kind.NewDevice
                current isReinstallOf previous -> AppHistoryEntry.Change.Kind.Reinstalled
                else -> AppHistoryEntry.Change.Kind.Updated
            }
            AppHistoryEntry.Change(
                kind = kind,
                snapshot = current.toSummary(devices),
                previous = previous.toSummary(devices),
                diff = differ(before = previous.record, after = current.record),
            )
        }

    private fun StoredAppHistoryRecord.toSummary(devices: Map<String, AppHistoryDevice>) = AppHistorySnapshotSummary(
        id = AppHistorySnapshotId(id),
        applicationName = record.detail.info.applicationName,
        version = record.detail.info.version,
        firstInstallTime = record.captureKey.firstInstallTime,
        lastUpdateTime = record.captureKey.lastUpdateTime,
        capturedAt = record.capturedAt,
        recordedOn = recordedOnResolver.resolve(record.captureKey.deviceId, devices),
    )

    private fun AppHistorySnapshotHeader.toSummary(devices: Map<String, AppHistoryDevice>) = AppHistorySnapshotSummary(
        id = AppHistorySnapshotId(id),
        applicationName = applicationName,
        version = AppHistoryVersion(versionCode = versionCode, versionName = versionName),
        firstInstallTime = captureKey.firstInstallTime,
        lastUpdateTime = captureKey.lastUpdateTime,
        capturedAt = capturedAt,
        recordedOn = recordedOnResolver.resolve(captureKey.deviceId, devices),
    )
}

private val AppHistoryEntry.Change.isUpdateWithoutVisibleChange: Boolean
    get() = kind == AppHistoryEntry.Change.Kind.Updated && !diff.isVisible

private val AppHistoryDiff.isVisible: Boolean
    get() = changedCategories.isNotEmpty() || uncomparableCategories.isNotEmpty()

private infix fun StoredAppHistoryRecord.isOnNewDeviceAfter(previous: StoredAppHistoryRecord): Boolean =
    record.captureKey.deviceId != previous.record.captureKey.deviceId

private infix fun StoredAppHistoryRecord.isReinstallOf(previous: StoredAppHistoryRecord): Boolean =
    record.captureKey.firstInstallTime != previous.record.captureKey.firstInstallTime

private fun <T> CapturedSection<T>.toSection(): AppHistorySection<T> = when (this) {
    is CapturedSection.Captured -> AppHistorySection.Recorded(value)
    CapturedSection.Unavailable -> AppHistorySection.NotRecorded
}
