package sk.styk.martin.apkanalyzer.core.apphistory.timeline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import sk.styk.martin.apkanalyzer.core.apphistory.capture.APP_HISTORY
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryPersistenceRepository
import sk.styk.martin.apkanalyzer.core.apphistory.storage.StoredAppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySnapshotId
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySnapshotSummary
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryTimeline
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppHistoryRepositoryImpl @Inject constructor(
    private val persistenceRepository: AppHistoryPersistenceRepository,
    private val differ: AppHistoryDiffer,
    private val dispatcherProvider: DispatcherProvider,
) : AppHistoryRepository {

    override fun timeline(packageName: PackageName): Flow<AppHistoryTimeline> = persistenceRepository.observe(packageName)
        .map { result ->
            result.fold(
                onSuccess = { records -> AppHistoryTimeline.Available(buildTimeline(records)) },
                onFailure = { throwable ->
                    Logger.e(APP_HISTORY, throwable, "Timeline load failed for ${packageName.value}")
                    AppHistoryTimeline.Unavailable
                },
            )
        }
        .flowOn(dispatcherProvider.default())

    private fun buildTimeline(records: List<StoredAppHistoryRecord>): List<AppHistoryEntry> {
        val first = records.firstOrNull() ?: return emptyList()
        val changes = records.zipWithNext { previous, current ->
            val diff = differ(before = previous.record, after = current.record)
            val kind = when {
                current isOnNewDeviceAfter previous -> AppHistoryEntry.Change.Kind.NewDevice
                current isReinstallOf previous -> AppHistoryEntry.Change.Kind.Reinstalled
                else -> AppHistoryEntry.Change.Kind.Updated
            }
            AppHistoryEntry.Change(kind = kind, snapshot = current.toSummary(), previous = previous.toSummary(), diff = diff)
        }.filterNot { it.isUpdateWithoutVisibleChange }
        return changes.asReversed() + AppHistoryEntry.FirstSeen(first.toSummary())
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
    version = record.detail.info.version,
    firstInstallTime = record.captureKey.firstInstallTime,
    lastUpdateTime = record.captureKey.lastUpdateTime,
    capturedAt = record.capturedAt,
)
