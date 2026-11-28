package sk.styk.martin.apkanalyzer.core.apphistory.api.storage

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.common.model.AppSize

interface AppHistoryStorageRepository {
    fun usage(): Flow<AppHistoryStorageUsage>

    suspend fun export(destination: Uri): Result<Unit>

    suspend fun import(source: Uri): Result<AppHistoryImport>
}

sealed interface AppHistoryStorageUsage {
    data class Available(val size: AppSize, val appCount: Int) : AppHistoryStorageUsage

    data object Unavailable : AppHistoryStorageUsage
}

data class AppHistoryImport(val importedAppCount: Int)
