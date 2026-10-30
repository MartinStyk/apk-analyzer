package sk.styk.martin.apkanalyzer.core.apphistory.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.AppHistoryGateDao
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.AppHistoryGateState
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.AppHistoryReadDao
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.AppHistoryWriteDao
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistoryDeviceEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.util.AppHistoryRecordDecoder
import sk.styk.martin.apkanalyzer.core.apphistory.storage.util.AppHistoryRecordEncoder
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppHistoryPersistenceRepositoryImpl @Inject constructor(
    private val appHistoryGateDao: AppHistoryGateDao,
    private val appHistoryWriteDao: AppHistoryWriteDao,
    private val appHistoryReadDao: AppHistoryReadDao,
    private val encoder: AppHistoryRecordEncoder,
    private val decoder: AppHistoryRecordDecoder,
    private val dispatcherProvider: DispatcherProvider,
) : AppHistoryPersistenceRepository {

    override suspend fun latestCaptureKey(packageName: PackageName): Result<AppHistoryCaptureKey?> = runCatchingCancellable {
        appHistoryGateDao.latestGateState(packageName.value)?.toCaptureKey()
    }

    override suspend fun latestCaptureKeys(): Result<Map<PackageName, AppHistoryCaptureKey>> = runCatchingCancellable {
        appHistoryGateDao.latestGateStatesForAllPackages().associate { PackageName(it.packageName) to it.toCaptureKey() }
    }

    override suspend fun save(record: AppHistoryRecord, device: AppHistoryDevice): Result<Unit> = runCatchingCancellable {
        val encoded = encoder.encode(record)
        appHistoryWriteDao.insertSnapshotWithBlobs(device.toEntity(record), encoded.snapshot, encoded.blobs)
    }

    override fun observe(packageName: PackageName): Flow<Result<List<StoredAppHistoryRecord>>> = appHistoryReadDao.observeSnapshots(packageName.value)
        .distinctUntilChanged()
        .mapLatest { rows -> runCatchingCancellable { decoder.decode(rows, appHistoryReadDao.blobs(packageName.value)) } }
        .catch { emit(Result.failure(it)) }
        .flowOn(dispatcherProvider.default())
}

private fun AppHistoryGateState.toCaptureKey() = AppHistoryCaptureKey(
    deviceId = deviceId,
    firstInstallTime = Instant.ofEpochMilli(firstInstallTime),
    lastUpdateTime = Instant.ofEpochMilli(lastUpdateTime),
    versionCode = versionCode,
)

private fun AppHistoryDevice.toEntity(record: AppHistoryRecord) = AppHistoryDeviceEntity(
    deviceId = record.captureKey.deviceId,
    firstSeenAt = record.capturedAt.toEpochMilli(),
    manufacturer = manufacturer,
    model = model,
)
