package sk.styk.martin.apkanalyzer.core.apphistory.timeline.model

import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import java.time.Instant

val AppHistoryDiff.hasCertificateChange: Boolean
    get() = certificates is SetChange.Changed

val AppHistoryEntry.Change.isOlderVersion: Boolean
    get() = diff.version?.let { it.after.versionCode < it.before.versionCode } == true

val AppHistoryEntry.Change.isSameVersionBuild: Boolean
    get() = kind == AppHistoryEntry.Change.Kind.Updated && diff.version == null

val AppHistoryEntry.Change.hasOnlyUncomparableDetails: Boolean
    get() = diff.changedCategories.isEmpty() && diff.uncomparableCategories.isNotEmpty()

val AppHistoryEntry.Change.isQuiet: Boolean
    get() = kind == AppHistoryEntry.Change.Kind.Updated &&
        !diff.hasCertificateChange &&
        !hasOnlyUncomparableDetails &&
        summaryParts { it }.all { it is AppHistorySummaryPart.SizeChanged }

sealed interface AppHistorySummaryPart {
    data object SigningMethodChanged : AppHistorySummaryPart

    data class InstallSourceChanged(val change: ValueChange<AppHistoryInstallSource>) : AppHistorySummaryPart

    data class DebuggingChanged(val isEnabled: Boolean) : AppHistorySummaryPart

    data class BackupChanged(val isAllowed: Boolean) : AppHistorySummaryPart

    data class UnencryptedTrafficChanged(val isAllowed: Boolean) : AppHistorySummaryPart

    data class PermissionsAdded(val firstLabel: String, val otherCount: Int) : AppHistorySummaryPart

    data class PermissionsRemoved(val count: Int) : AppHistorySummaryPart

    data class DefinedPermissionsChanged(val count: Int) : AppHistorySummaryPart

    data class TargetSdkChanged(val before: Int?, val after: Int?) : AppHistorySummaryPart

    data class MinSdkChanged(val before: Int?, val after: Int?) : AppHistorySummaryPart

    data class ComponentsAdded(val count: Int) : AppHistorySummaryPart

    data class ComponentsRemoved(val count: Int) : AppHistorySummaryPart

    data class ComponentsChanged(val count: Int) : AppHistorySummaryPart

    data class NativeLibrariesAdded(val count: Int) : AppHistorySummaryPart

    data class NativeLibrariesRemoved(val count: Int) : AppHistorySummaryPart

    data class NativeLibrariesChanged(val count: Int) : AppHistorySummaryPart

    data object RequirementsChanged : AppHistorySummaryPart

    data class SizeChanged(val before: AppSize, val after: AppSize) : AppHistorySummaryPart
}

fun AppHistoryEntry.Change.summaryParts(permissionLabel: (String) -> String): List<AppHistorySummaryPart> = buildList {
    if (!diff.hasCertificateChange && diff.signingSchemes is SetChange.Changed) add(AppHistorySummaryPart.SigningMethodChanged)
    diff.installSource?.let { add(AppHistorySummaryPart.InstallSourceChanged(it)) }
    diff.isDebuggable?.let { add(AppHistorySummaryPart.DebuggingChanged(it.after)) }
    addPermissionParts(diff, permissionLabel)
    diff.allowsBackup?.let { add(AppHistorySummaryPart.BackupChanged(it.after)) }
    diff.usesCleartextTraffic?.let { add(AppHistorySummaryPart.UnencryptedTrafficChanged(it.after)) }
    diff.targetSdkVersion?.let { add(AppHistorySummaryPart.TargetSdkChanged(it.before, it.after)) }
    diff.minSdkVersion?.let { add(AppHistorySummaryPart.MinSdkChanged(it.before, it.after)) }
    addComponentParts(diff)
    addNativeLibraryParts(diff)
    if (diff.requirements is SetChange.Changed) add(AppHistorySummaryPart.RequirementsChanged)
    diff.apkSize?.let { add(AppHistorySummaryPart.SizeChanged(it.before, it.after)) }
}

private fun MutableList<AppHistorySummaryPart>.addPermissionParts(diff: AppHistoryDiff, permissionLabel: (String) -> String) {
    (diff.requestedPermissions as? SetChange.Changed)?.let { requested ->
        val added = requested.added.distinctBy { it.permissionData.name }.sortedByDescending { it.isDangerous }
        added.firstOrNull()?.let { first ->
            add(AppHistorySummaryPart.PermissionsAdded(permissionLabel(first.permissionData.name), added.size - 1))
        }
        val removedCount = requested.removed.distinctBy { it.permissionData.name }.size
        if (removedCount > 0) add(AppHistorySummaryPart.PermissionsRemoved(removedCount))
    }
    (diff.definedPermissions as? SetChange.Changed)?.let { defined ->
        add(AppHistorySummaryPart.DefinedPermissionsChanged(defined.added.size + defined.removed.size + defined.modified.size))
    }
}

private fun MutableList<AppHistorySummaryPart>.addComponentParts(diff: AppHistoryDiff) {
    val changes = listOf(diff.activities, diff.services, diff.receivers, diff.providers).filterIsInstance<SetChange.Changed<*>>()
    val added = changes.sumOf { it.added.size }
    val removed = changes.sumOf { it.removed.size }
    val modified = changes.sumOf { it.modified.size }
    if (added > 0) add(AppHistorySummaryPart.ComponentsAdded(added))
    if (removed > 0) add(AppHistorySummaryPart.ComponentsRemoved(removed))
    if (modified > 0) add(AppHistorySummaryPart.ComponentsChanged(modified))
}

private fun MutableList<AppHistorySummaryPart>.addNativeLibraryParts(diff: AppHistoryDiff) {
    val libraries = diff.nativeLibraries as? SetChange.Changed ?: return
    val added = libraries.added.size
    val removed = libraries.removed.size
    val modified = libraries.modified.size
    if (added > 0) add(AppHistorySummaryPart.NativeLibrariesAdded(added))
    if (removed > 0) add(AppHistorySummaryPart.NativeLibrariesRemoved(removed))
    if (modified > 0) add(AppHistorySummaryPart.NativeLibrariesChanged(modified))
}

sealed interface AppHistoryOverview {
    data object NoEntries : AppHistoryOverview

    data class FirstRecordedOnly(val recordingSince: Instant) : AppHistoryOverview

    data class Changes(val count: Int, val latestAt: Instant) : AppHistoryOverview

    data class CertificateChanged(val changedAt: Instant) : AppHistoryOverview
}

val List<AppHistoryEntry>.changeCount: Int
    get() = count { it is AppHistoryEntry.Change }

val List<AppHistoryEntry>.recordingSince: Instant?
    get() = lastOrNull()?.snapshot?.capturedAt

val List<AppHistoryEntry>.overview: AppHistoryOverview
    get() {
        val changes = filterIsInstance<AppHistoryEntry.Change>()
        val latestCertificateChange = changes.firstOrNull { it.diff.hasCertificateChange }
        val since = recordingSince
        return when {
            latestCertificateChange != null -> AppHistoryOverview.CertificateChanged(latestCertificateChange.timestamp)
            changes.isNotEmpty() -> AppHistoryOverview.Changes(count = changes.size, latestAt = changes.first().timestamp)
            since != null -> AppHistoryOverview.FirstRecordedOnly(since)
            else -> AppHistoryOverview.NoEntries
        }
    }
