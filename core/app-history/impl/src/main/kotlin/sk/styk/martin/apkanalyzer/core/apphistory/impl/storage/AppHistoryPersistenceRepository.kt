package sk.styk.martin.apkanalyzer.core.apphistory.impl.storage

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apps.api.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

internal interface AppHistoryPersistenceRepository {
    suspend fun latestCaptureKey(packageName: PackageName): Result<AppHistoryCaptureKey?>
    suspend fun latestCaptureKeys(): Result<Map<PackageName, AppHistoryCaptureKey>>
    suspend fun save(record: AppHistoryRecord, device: AppHistoryDevice): Result<Unit>
    fun observe(packageName: PackageName): Flow<Result<List<StoredAppHistoryRecord>>>
    fun observeHeaders(): Flow<Result<List<AppHistorySnapshotHeader>>>
    suspend fun records(packageName: PackageName): Result<List<StoredAppHistoryRecord>>
    suspend fun read(id: Long): Result<StoredAppHistoryRecord?>
    suspend fun devices(): Result<Map<String, AppHistoryDevice>>
}

internal data class AppHistorySnapshotHeader(
    val id: Long,
    val packageName: PackageName,
    val applicationName: String,
    val versionCode: Long,
    val versionName: String?,
    val capturedAt: Instant,
    val captureKey: AppHistoryCaptureKey,
)

internal data class AppHistoryCaptureKey(
    val deviceId: String,
    val firstInstallTime: Instant,
    val lastUpdateTime: Instant,
    val versionCode: Long,
)

internal data class AppHistoryDevice(val manufacturer: String, val model: String)

internal data class AppHistoryRecord(
    val packageName: PackageName,
    val capturedAt: Instant,
    val captureKey: AppHistoryCaptureKey,
    val detail: AppDetail,
    val nativeLibraries: CapturedSection<NativeLibraries>,
    val signingScheme: CapturedSection<List<SigningSchemeVersion>?>,
)

internal data class StoredAppHistoryRecord(val id: Long, val record: AppHistoryRecord)

internal sealed interface CapturedSection<out T> {
    data class Captured<T>(val value: T) : CapturedSection<T>

    data object Unavailable : CapturedSection<Nothing>
}
