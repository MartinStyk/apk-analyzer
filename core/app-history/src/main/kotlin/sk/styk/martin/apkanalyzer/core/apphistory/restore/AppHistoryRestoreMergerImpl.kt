package sk.styk.martin.apkanalyzer.core.apphistory.restore

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apphistory.capture.APP_HISTORY
import sk.styk.martin.apkanalyzer.core.apphistory.storage.APP_HISTORY_STAGING_DATABASE_NAME
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryDatabase
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryWriteDao
import sk.styk.martin.apkanalyzer.core.apphistory.storage.entity.AppHistoryBlobEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.entity.AppHistorySnapshotEntity
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppHistoryRestoreMergerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val liveWriteDao: AppHistoryWriteDao,
    private val dispatcherProvider: DispatcherProvider,
) : AppHistoryRestoreMerger {

    private data class StagedData(val snapshots: List<AppHistorySnapshotEntity>, val blobs: List<AppHistoryBlobEntity>)

    override suspend fun mergeIfPending(): Result<Unit> = withContext(dispatcherProvider.io()) {
        if (!context.getDatabasePath(APP_HISTORY_STAGING_DATABASE_NAME).exists()) {
            return@withContext Result.success(Unit)
        }

        Logger.i(APP_HISTORY, "Restore merge started")

        val staged = runCatchingCancellable { readStagedData() }
            .onFailure {
                Logger.w(APP_HISTORY, it, "Restore merge failed to read the staged backup, discarding it")
                deleteStagingFiles()
            }
        val (snapshots, blobs) = staged.getOrElse { return@withContext Result.failure(it) }

        runCatchingCancellable { liveWriteDao.mergeSnapshotsWithBlobs(snapshots, blobs) }
            .onSuccess {
                Logger.i(APP_HISTORY, "Restore merge successful")
                deleteStagingFiles()
            }
            .onFailure {
                Logger.w(APP_HISTORY, it, "Restore merge failed to write to the live database, will retry on next launch")
            }
    }

    private suspend fun readStagedData(): StagedData {
        val stagingDatabase = Room.databaseBuilder(context, AppHistoryDatabase::class.java, APP_HISTORY_STAGING_DATABASE_NAME).build()
        return try {
            val readDao = stagingDatabase.appHistoryReadDao()
            StagedData(readDao.allSnapshots().map { it.copy(id = 0) }, readDao.allBlobs())
        } finally {
            stagingDatabase.close()
        }
    }

    private fun deleteStagingFiles() {
        listOf(APP_HISTORY_STAGING_DATABASE_NAME, "$APP_HISTORY_STAGING_DATABASE_NAME-wal", "$APP_HISTORY_STAGING_DATABASE_NAME-shm")
            .forEach { context.getDatabasePath(it).delete() }
    }
}
