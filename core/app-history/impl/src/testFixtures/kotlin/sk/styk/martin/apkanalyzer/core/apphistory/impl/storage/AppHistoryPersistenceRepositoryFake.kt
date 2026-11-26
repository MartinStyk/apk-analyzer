package sk.styk.martin.apkanalyzer.core.apphistory.impl.storage

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

internal class AppHistoryPersistenceRepositoryFake(vararg records: StoredAppHistoryRecord) : AppHistoryPersistenceRepository {
    private val records = MutableStateFlow(records.toList())

    var readFailure: Throwable? = null
    var saveFailure: Throwable? = null
    var saveBarrier: Deferred<Unit>? = null

    operator fun plusAssign(record: StoredAppHistoryRecord) {
        records.update { existing -> existing.filterNot { it.id == record.id } + record }
    }

    override suspend fun latestCaptureKey(packageName: PackageName): Result<AppHistoryCaptureKey?> =
        Result.success(records.value.lastOrNull { it.record.packageName == packageName }?.record?.captureKey)

    override suspend fun latestCaptureKeys(): Result<Map<PackageName, AppHistoryCaptureKey>> =
        Result.success(records.value.associate { it.record.packageName to it.record.captureKey })

    override suspend fun save(record: AppHistoryRecord, device: AppHistoryDevice): Result<Unit> {
        saveBarrier?.await()
        saveFailure?.let { return Result.failure(it) }
        this += StoredAppHistoryRecord(id = records.value.size + 1L, record = record)
        return Result.success(Unit)
    }

    override fun observe(packageName: PackageName): Flow<Result<List<StoredAppHistoryRecord>>> =
        records.map { all -> Result.success(all.filter { it.record.packageName == packageName }) }.distinctUntilChanged()

    override suspend fun read(id: Long): Result<StoredAppHistoryRecord?> =
        readFailure?.let { Result.failure(it) } ?: Result.success(records.value.firstOrNull { it.id == id })
}
