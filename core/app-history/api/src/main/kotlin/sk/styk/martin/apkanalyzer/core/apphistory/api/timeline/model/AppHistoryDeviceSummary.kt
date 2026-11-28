package sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model

import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

data class AppHistoryDeviceChange(
    val packageName: PackageName,
    val applicationName: String,
    val change: AppHistoryEntry.Change,
)

data class AppHistoryAttentionItem(val change: AppHistoryDeviceChange, val revertedAt: Instant?)

data class AppHistoryChangedApp(
    val packageName: PackageName,
    val applicationName: String,
    val updateCount: Int,
    val isNew: Boolean,
    val netChange: AppHistoryDiff?,
    val hasCertificateWarning: Boolean,
    val otherDevices: List<String>,
    val latestAt: Instant,
)

data class AppHistoryDeviceSummary(
    val since: Instant,
    val updateCount: Int,
    val newAppCount: Int,
    val needsAttention: List<AppHistoryAttentionItem>,
    val changedApps: List<AppHistoryChangedApp>,
    val otherDevices: List<String>,
)

val AppHistoryDeviceTimeline.Available.recordingSince: Instant?
    get() = apps.mapNotNull { app -> app.firstSeen?.snapshot?.capturedAt }.minOrNull()

val AppHistoryAppTimeline.lastSeenOn: AppHistoryRecordedOn
    get() = entries.firstOrNull()?.snapshot?.recordedOn ?: AppHistoryRecordedOn.ThisDevice

val AppHistoryDeviceTimeline.Available.latestChange: AppHistoryDeviceChange?
    get() = apps.mapNotNull { app -> app.appChanges.firstOrNull()?.let { app.deviceChange(it) } }.maxByOrNull { it.change.timestamp }

val AppHistoryDeviceTimeline.Available.allEntries: List<AppHistoryDeviceEntry>
    get() = apps.flatMap { app -> app.entries.map { AppHistoryDeviceEntry(app.packageName, app.applicationName, it) } }
        .sortedByDescending { it.entry.occurredAt }

data class AppHistoryDeviceEntry(
    val packageName: PackageName,
    val applicationName: String,
    val entry: AppHistoryEntry,
)

fun AppHistoryDeviceTimeline.Available.summarySince(since: Instant): AppHistoryDeviceSummary {
    val changedApps = apps.mapNotNull { it.changedSince(since) }.sortedByDescending { it.latestAt }
    return AppHistoryDeviceSummary(
        since = since,
        updateCount = changedApps.sumOf { it.updateCount },
        newAppCount = changedApps.count { it.isNew },
        needsAttention = apps.mapNotNull { it.attentionSince(since) }.sortedByDescending { it.change.change.timestamp },
        changedApps = changedApps,
        otherDevices = apps.flatMap { app -> app.entries.filter { it.occurredAt >= since }.flatMap { it.devices } }.otherDeviceNames(),
    )
}

private val AppHistoryEntry.devices: List<AppHistoryRecordedOn>
    get() = when (this) {
        is AppHistoryEntry.FirstSeen -> listOf(snapshot.recordedOn)
        is AppHistoryEntry.Change -> listOf(snapshot.recordedOn, previous.recordedOn)
    }

private fun List<AppHistoryRecordedOn>.otherDeviceNames(): List<String> = filterIsInstance<AppHistoryRecordedOn.OtherDevice>().map { it.name }.distinct()

private val AppHistoryAppTimeline.firstSeen: AppHistoryEntry.FirstSeen?
    get() = entries.lastOrNull() as? AppHistoryEntry.FirstSeen

private val AppHistoryAppTimeline.changes: List<AppHistoryEntry.Change>
    get() = entries.filterIsInstance<AppHistoryEntry.Change>()

private val AppHistoryAppTimeline.appChanges: List<AppHistoryEntry.Change>
    get() = changes.filter { it.kind != AppHistoryEntry.Change.Kind.NewDevice }

private fun AppHistoryAppTimeline.deviceChange(change: AppHistoryEntry.Change) = AppHistoryDeviceChange(packageName, applicationName, change)

private fun AppHistoryAppTimeline.changedSince(since: Instant): AppHistoryChangedApp? {
    val changesInRange = appChanges.filter { it.timestamp >= since }
    val unchangedInRange = unchangedUpdates.filter { it >= since }
    val installedAt = firstSeen?.snapshot?.firstInstallTime?.takeIf { it >= since }
    val reinstall = changesInRange.firstOrNull { it.kind == AppHistoryEntry.Change.Kind.Reinstalled }
    val isNew = installedAt != null || reinstall != null
    val updateCount = changesInRange.count { it.kind == AppHistoryEntry.Change.Kind.Updated } + unchangedInRange.size
    if (!isNew && updateCount == 0) return null
    val baseline = reinstall?.snapshot?.id ?: changesInRange.lastOrNull()?.previous?.id ?: firstSeen?.snapshot?.id.takeIf { isNew }
    val netChange = baseline?.let { diffsToLatest[it] }
    val newInstall = firstSeen?.takeIf { installedAt != null }
    return AppHistoryChangedApp(
        packageName = packageName,
        applicationName = applicationName,
        updateCount = updateCount,
        isNew = isNew,
        netChange = netChange,
        hasCertificateWarning = netChange?.hasCertificateChange == true && changesInRange.any { it.hasCertificateWarning },
        otherDevices = (changesInRange + listOfNotNull(newInstall)).map { it.snapshot.recordedOn }.otherDeviceNames(),
        latestAt = listOfNotNull(changesInRange.firstOrNull()?.timestamp, unchangedInRange.maxOrNull(), installedAt).max(),
    )
}

private fun AppHistoryAppTimeline.attentionSince(since: Instant): AppHistoryAttentionItem? {
    val newestFirst = changes
    val index = newestFirst.indexOfFirst { it.timestamp >= since && it.needsAttention }.takeIf { it >= 0 } ?: return null
    val change = newestFirst[index]
    return AppHistoryAttentionItem(
        change = deviceChange(change),
        revertedAt = change.revertedBy(laterChanges = newestFirst.subList(0, index).asReversed()),
    )
}

private fun AppHistoryEntry.Change.revertedBy(laterChanges: List<AppHistoryEntry.Change>): Instant? {
    val reasons = summaryParts { it }.filter { it.needsAttention }
    if (diff.hasCertificateChange || reasons.any { it is AppHistorySummaryPart.SizeChanged }) return null
    val revertTimes = buildList {
        if (reasons.any { it is AppHistorySummaryPart.DangerousPermissionsAdded }) add(diff.dangerousPermissionsRemovedBy(laterChanges))
        diff.installSource?.let { change -> add(laterChanges.revertTime(change.before) { it.diff.installSource?.after }) }
        if (diff.isDebuggable?.after == true) add(laterChanges.revertTime(false) { it.diff.isDebuggable?.after })
    }
    return if (revertTimes.isNotEmpty() && revertTimes.all { it != null }) revertTimes.filterNotNull().max() else null
}

private fun AppHistoryDiff.dangerousPermissionsRemovedBy(laterChanges: List<AppHistoryEntry.Change>): Instant? {
    val names = addedDangerousPermissions.map { it.permissionData.name }.toSet()
    val remaining = names.toMutableSet()
    var revertedAt: Instant? = null
    laterChanges.forEach { later ->
        val requested = later.diff.requestedPermissions as? SetChange.Changed ?: return@forEach
        remaining -= requested.removed.map { it.permissionData.name }.toSet()
        remaining += requested.added.map { it.permissionData.name }.filter { it in names }
        revertedAt = if (remaining.isEmpty()) revertedAt ?: later.timestamp else null
    }
    return revertedAt
}

private fun <T> List<AppHistoryEntry.Change>.revertTime(original: T, valueAfter: (AppHistoryEntry.Change) -> T?): Instant? {
    var revertedAt: Instant? = null
    forEach { later ->
        val value = valueAfter(later) ?: return@forEach
        revertedAt = if (value == original) later.timestamp else null
    }
    return revertedAt
}
