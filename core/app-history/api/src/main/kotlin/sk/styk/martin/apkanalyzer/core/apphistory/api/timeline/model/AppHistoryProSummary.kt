package sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model

import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

object AppHistoryFreeSample {
    const val SECTION_ROWS = 5
    const val LOG_ROWS = 10
}

fun AppHistoryChangedApp.isFreeAt(index: Int): Boolean = index < AppHistoryFreeSample.SECTION_ROWS || netChange?.hasCertificateChange == true

val AppHistoryEntry.isFreeSample: Boolean
    get() = when (this) {
        is AppHistoryEntry.FirstSeen -> true
        is AppHistoryEntry.Change -> diff.hasCertificateChange
    }

data class AppHistoryProSummary(
    val since: Instant,
    val changeCount: Int,
    val changedAppCount: Int,
    val freeAppCount: Int,
    val lockedAppCount: Int,
    val addedPermissionCount: Int,
    val componentChangeCount: Int,
    val sizeChangeCount: Int,
    val latestLocked: AppHistoryLockedPreview?,
)

data class AppHistoryLockedPreview(
    val packageName: PackageName,
    val applicationName: String,
    val version: ValueChange<AppHistoryVersion>?,
    val addedPermissionCount: Int,
    val componentChangeCount: Int,
    val isSizeChanged: Boolean,
)

fun AppHistoryDeviceTimeline.Available.proSummary(): AppHistoryProSummary? {
    val since = recordingSince ?: return null
    val changedApps = summarySince(since).changedApps
    val lockedApps = changedApps.filterIndexed { index, app -> !app.isFreeAt(index) }
    val diffs = apps.flatMap { app ->
        app.entries.filterIsInstance<AppHistoryEntry.Change>().filter { it.kind != AppHistoryEntry.Change.Kind.NewDevice }.map { it.diff }
    }
    return AppHistoryProSummary(
        since = since,
        changeCount = diffs.size,
        changedAppCount = changedApps.size,
        freeAppCount = changedApps.size - lockedApps.size,
        lockedAppCount = lockedApps.size,
        addedPermissionCount = diffs.sumOf { it.requestedPermissions.addedCount },
        componentChangeCount = diffs.sumOf { it.componentChangeCount },
        sizeChangeCount = diffs.count { it.apkSize != null },
        latestLocked = lockedApps.firstOrNull()?.toLockedPreview(),
    )
}

private fun AppHistoryChangedApp.toLockedPreview() = AppHistoryLockedPreview(
    packageName = packageName,
    applicationName = applicationName,
    version = netChange?.version,
    addedPermissionCount = netChange?.requestedPermissions?.addedCount ?: 0,
    componentChangeCount = netChange?.componentChangeCount ?: 0,
    isSizeChanged = netChange?.apkSize != null,
)

private val AppHistoryDiff.componentChangeCount: Int
    get() = activities.changeCount + services.changeCount + receivers.changeCount + providers.changeCount

private val SetChange<*>.addedCount: Int
    get() = (this as? SetChange.Changed<*>)?.added?.size ?: 0

private val SetChange<*>.changeCount: Int
    get() = (this as? SetChange.Changed<*>)?.let { it.added.size + it.removed.size + it.modified.size } ?: 0
