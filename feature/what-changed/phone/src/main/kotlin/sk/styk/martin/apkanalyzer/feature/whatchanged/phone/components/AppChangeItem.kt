package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryChangeCategory
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySummaryPart
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.hasCertificateChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.hasOnlyUncomparableDetails
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.needsAttention
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.summaryParts
import sk.styk.martin.apkanalyzer.core.apppermissions.api.PermissionLabelProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.time.Instant
import javax.inject.Inject

@Immutable
internal sealed interface AppChangeKind {
    data object FirstRecorded : AppChangeKind

    data class Change(val kind: AppHistoryEntry.Change.Kind) : AppChangeKind
}

@Immutable
internal data class AppChangeItem(
    val packageName: String,
    val appName: String,
    val snapshotId: Long,
    val capturedAt: Instant,
    val date: Instant,
    val kind: AppChangeKind,
    val version: AppHistoryVersion,
    val versionChange: ValueChange<AppHistoryVersion>? = null,
    val parts: List<AppHistorySummaryPart> = emptyList(),
    val categories: Set<AppHistoryChangeCategory> = emptySet(),
    val hasCertificateChange: Boolean = false,
    val needsAttention: Boolean = false,
    val hasOnlyUncomparableDetails: Boolean = false,
    val revertedAt: Instant? = null,
) {
    val key: String
        get() = "$packageName:$snapshotId"

    val historicalInput: AppDetailInput.Historical
        get() = AppDetailInput.Historical(packageName = packageName, snapshotId = snapshotId, capturedAt = capturedAt)
}

internal class AppChangeItemMapper @Inject constructor(private val permissionLabelProvider: PermissionLabelProvider) {

    fun map(
        packageName: PackageName,
        appName: String,
        entry: AppHistoryEntry,
        revertedAt: Instant? = null,
    ): AppChangeItem = when (entry) {
        is AppHistoryEntry.FirstSeen -> AppChangeItem(
            packageName = packageName.value,
            appName = appName,
            snapshotId = entry.snapshot.id.value,
            capturedAt = entry.snapshot.capturedAt,
            date = entry.snapshot.capturedAt,
            kind = AppChangeKind.FirstRecorded,
            version = entry.snapshot.version,
        )

        is AppHistoryEntry.Change -> AppChangeItem(
            packageName = packageName.value,
            appName = appName,
            snapshotId = entry.snapshot.id.value,
            capturedAt = entry.snapshot.capturedAt,
            date = entry.timestamp,
            kind = AppChangeKind.Change(entry.kind),
            version = entry.snapshot.version,
            versionChange = entry.diff.version,
            parts = entry.summaryParts(permissionLabelProvider::getLabel).sortedByDescending { it.needsAttention },
            categories = entry.diff.changedCategories,
            hasCertificateChange = entry.diff.hasCertificateChange,
            needsAttention = entry.needsAttention,
            hasOnlyUncomparableDetails = entry.hasOnlyUncomparableDetails,
            revertedAt = revertedAt,
        )
    }

    fun permissionLabel(permissionName: String): String = permissionLabelProvider.getLabel(permissionName)
}
