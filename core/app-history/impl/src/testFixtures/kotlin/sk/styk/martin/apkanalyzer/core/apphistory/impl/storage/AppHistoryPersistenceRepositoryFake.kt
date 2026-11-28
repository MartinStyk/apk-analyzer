package sk.styk.martin.apkanalyzer.core.apphistory.impl.storage

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

internal class AppHistoryPersistenceRepositoryFake(vararg records: StoredAppHistoryRecord) : AppHistoryPersistenceRepository {
    private val records = MutableStateFlow(records.toList())
    private val devices = mutableMapOf<String, AppHistoryDevice>()

    var readFailure: Throwable? = null
    var headersFailure: Throwable? = null
    var saveFailure: Throwable? = null
    var saveBarrier: Deferred<Unit>? = null
    val recordReads = mutableListOf<PackageName>()
    var headerObservers: Int = 0
        private set
    var headerObservationCount: Int = 0
        private set

    operator fun plusAssign(record: StoredAppHistoryRecord) {
        records.update { existing -> existing.filterNot { it.id == record.id } + record }
    }

    operator fun set(deviceId: String, device: AppHistoryDevice) {
        devices[deviceId] = device
    }

    override suspend fun latestCaptureKey(packageName: PackageName): Result<AppHistoryCaptureKey?> =
        Result.success(records.value.lastOrNull { it.record.packageName == packageName }?.record?.captureKey)

    override suspend fun latestCaptureKeys(): Result<Map<PackageName, AppHistoryCaptureKey>> =
        Result.success(records.value.associate { it.record.packageName to it.record.captureKey })

    override suspend fun save(record: AppHistoryRecord, device: AppHistoryDevice): Result<Unit> {
        saveBarrier?.await()
        saveFailure?.let { return Result.failure(it) }
        devices.putIfAbsent(record.captureKey.deviceId, device)
        this += StoredAppHistoryRecord(id = records.value.size + 1L, record = record)
        return Result.success(Unit)
    }

    override fun observe(packageName: PackageName): Flow<Result<List<StoredAppHistoryRecord>>> =
        records.map { all -> Result.success(all.filter { it.record.packageName == packageName }) }.distinctUntilChanged()

    override fun observeHeaders(): Flow<Result<List<AppHistorySnapshotHeader>>> = records.map { all ->
        headersFailure?.let { Result.failure(it) } ?: Result.success(
            all.sortedWith(compareBy({ it.record.packageName.value }, { it.record.capturedAt }, { it.id })).map { it.toHeader() },
        )
    }.onStart {
        headerObservers++
        headerObservationCount++
    }.onCompletion {
        headerObservers--
    }

    override suspend fun records(packageName: PackageName): Result<List<StoredAppHistoryRecord>> {
        recordReads += packageName
        return readFailure?.let { Result.failure(it) }
            ?: Result.success(records.value.filter { it.record.packageName == packageName }.sortedWith(compareBy({ it.record.capturedAt }, { it.id })))
    }

    override suspend fun read(id: Long): Result<StoredAppHistoryRecord?> =
        readFailure?.let { Result.failure(it) } ?: Result.success(records.value.firstOrNull { it.id == id })

    override suspend fun devices(): Result<Map<String, AppHistoryDevice>> = readFailure?.let { Result.failure(it) } ?: Result.success(devices.toMap())
}

private fun StoredAppHistoryRecord.toHeader() = AppHistorySnapshotHeader(
    id = id,
    packageName = record.packageName,
    applicationName = record.detail.info.applicationName,
    versionCode = record.detail.info.versionCode,
    versionName = record.detail.info.versionName,
    capturedAt = record.capturedAt,
    captureKey = record.captureKey,
)
