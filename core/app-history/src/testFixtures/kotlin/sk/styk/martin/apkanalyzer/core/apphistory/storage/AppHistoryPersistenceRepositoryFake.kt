package sk.styk.martin.apkanalyzer.core.apphistory.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

internal class AppHistoryPersistenceRepositoryFake(vararg records: StoredAppHistoryRecord) : AppHistoryPersistenceRepository {
    private val records = records.toMutableList()

    var readFailure: Throwable? = null

    operator fun plusAssign(record: StoredAppHistoryRecord) {
        records.removeAll { it.id == record.id }
        records += record
    }

    override suspend fun latestCaptureKey(packageName: PackageName): Result<AppHistoryCaptureKey?> =
        Result.success(records.lastOrNull { it.record.packageName == packageName }?.record?.captureKey)

    override suspend fun latestCaptureKeys(): Result<Map<PackageName, AppHistoryCaptureKey>> =
        Result.success(records.associate { it.record.packageName to it.record.captureKey })

    override suspend fun save(record: AppHistoryRecord, device: AppHistoryDevice): Result<Unit> {
        this += StoredAppHistoryRecord(id = records.size + 1L, record = record)
        return Result.success(Unit)
    }

    override fun observe(packageName: PackageName): Flow<Result<List<StoredAppHistoryRecord>>> =
        flowOf(Result.success(records.filter { it.record.packageName == packageName }))

    override suspend fun read(id: Long): Result<StoredAppHistoryRecord?> =
        readFailure?.let { Result.failure(it) } ?: Result.success(records.firstOrNull { it.id == id })
}
