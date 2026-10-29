package sk.styk.martin.apkanalyzer.core.apphistory.storage

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

internal interface AppHistoryPersistenceRepository {
    suspend fun latestCaptureKey(packageName: PackageName): Result<AppHistoryCaptureKey?>
    suspend fun latestCaptureKeys(): Result<Map<PackageName, AppHistoryCaptureKey>>
    suspend fun save(record: AppHistoryRecord): Result<Unit>
    fun observe(packageName: PackageName): Flow<Result<List<StoredAppHistoryRecord>>>
}

internal data class AppHistoryCaptureKey(
    val deviceId: String,
    val firstInstallTime: Instant,
    val lastUpdateTime: Instant,
)

internal data class AppHistoryRecord(
    val packageName: PackageName,
    val capturedAt: Instant,
    val captureKey: AppHistoryCaptureKey,
    val detail: AppDetail,
    val intentFilters: CapturedSection<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>>,
    val nativeLibraries: CapturedSection<NativeLibraries>,
    val signingScheme: CapturedSection<List<SigningSchemeVersion>?>,
)

internal data class StoredAppHistoryRecord(val id: Long, val record: AppHistoryRecord)

internal sealed interface CapturedSection<out T> {
    data class Captured<T>(val value: T) : CapturedSection<T>

    data object Unavailable : CapturedSection<Nothing>
}
