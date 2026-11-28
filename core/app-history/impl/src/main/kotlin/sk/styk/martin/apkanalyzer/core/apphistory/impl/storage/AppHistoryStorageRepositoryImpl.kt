package sk.styk.martin.apkanalyzer.core.apphistory.impl.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apphistory.api.storage.AppHistoryImport
import sk.styk.martin.apkanalyzer.core.apphistory.api.storage.AppHistoryStorageRepository
import sk.styk.martin.apkanalyzer.core.apphistory.api.storage.AppHistoryStorageUsage
import sk.styk.martin.apkanalyzer.core.apphistory.impl.capture.APP_HISTORY
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.APP_HISTORY_DATABASE_NAME
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.APP_HISTORY_EXPORT_DATABASE_NAME
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.APP_HISTORY_IMPORT_DATABASE_NAME
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.AppHistoryDatabase
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.AppHistoryReadDao
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.AppHistoryWriteDao
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.file.DocumentFileManager
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val IGNORED_ROW_ID = -1L

@Singleton
internal class AppHistoryStorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val liveReadDao: AppHistoryReadDao,
    private val liveWriteDao: AppHistoryWriteDao,
    private val documentFileManager: DocumentFileManager,
    private val dispatcherProvider: DispatcherProvider,
) : AppHistoryStorageRepository {

    private val transferMutex = Mutex()

    override fun usage(): Flow<AppHistoryStorageUsage> = liveReadDao.observeRecordedAppCount()
        .map<Int, AppHistoryStorageUsage> { appCount -> AppHistoryStorageUsage.Available(size = liveDatabaseSize(), appCount = appCount) }
        .catch {
            Logger.w(APP_HISTORY, it, "History storage usage could not be read")
            emit(AppHistoryStorageUsage.Unavailable)
        }
        .flowOn(dispatcherProvider.io())

    override suspend fun export(destination: Uri): Result<Unit> = transferMutex.withLock {
        withContext(dispatcherProvider.io()) {
            runCatchingCancellable {
                val exportFile = transferFile(APP_HISTORY_EXPORT_DATABASE_NAME)
                try {
                    val exportedAppCount = documentFileManager.writeOrDeleteOnFailure(destination) { output ->
                        val appCount = withTransferDatabase(exportFile) { exportDatabase ->
                            copyHistory(from = liveReadDao, to = exportDatabase.appHistoryWriteDao())
                        }
                        exportFile.inputStream().use { it.copyTo(output) }
                        appCount
                    }.getOrThrow()
                    Logger.i(APP_HISTORY, "History export finished with $exportedAppCount apps")
                } finally {
                    SQLiteDatabase.deleteDatabase(exportFile)
                }
            }.onFailure {
                Logger.w(APP_HISTORY, it, "History export failed")
            }
        }
    }

    override suspend fun import(source: Uri): Result<AppHistoryImport> = transferMutex.withLock {
        withContext(dispatcherProvider.io()) {
            runCatchingCancellable {
                val importFile = transferFile(APP_HISTORY_IMPORT_DATABASE_NAME)
                try {
                    documentFileManager.read(source) { input ->
                        importFile.outputStream().use { input.copyTo(it) }
                    }.getOrThrow()
                    requireRecordedHistory(importFile)
                    val importedAppCount = withTransferDatabase(importFile) { importDatabase ->
                        copyHistory(from = importDatabase.appHistoryReadDao(), to = liveWriteDao)
                    }
                    Logger.i(APP_HISTORY, "History import finished with $importedAppCount apps gaining history")
                    AppHistoryImport(importedAppCount = importedAppCount)
                } finally {
                    SQLiteDatabase.deleteDatabase(importFile)
                }
            }.onFailure {
                Logger.w(APP_HISTORY, it, "History import failed")
            }
        }
    }

    private fun liveDatabaseSize(): AppSize {
        val database = context.getDatabasePath(APP_HISTORY_DATABASE_NAME)
        return listOf(database, File("${database.path}-wal"), File("${database.path}-shm")).sumOf { it.length() }.bytes
    }

    private fun transferFile(name: String): File = File(context.noBackupFilesDir, name).also { SQLiteDatabase.deleteDatabase(it) }

    private fun requireRecordedHistory(file: File) {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { database ->
            database.rawQuery("SELECT identity_hash FROM room_master_table", null).use { cursor ->
                require(cursor.moveToFirst()) { "The file holds no recorded history" }
            }
        }
    }

    private suspend fun <T> withTransferDatabase(file: File, block: suspend (AppHistoryDatabase) -> T): T {
        val database = Room.databaseBuilder(context, AppHistoryDatabase::class.java, file.absolutePath)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
        return try {
            block(database)
        } finally {
            database.close()
        }
    }

    private suspend fun copyHistory(from: AppHistoryReadDao, to: AppHistoryWriteDao): Int {
        val snapshotsByPackage = from.allSnapshots().map { it.copy(id = 0) }.groupBy { it.packageName }
        val devices = from.allDevices()
        return snapshotsByPackage.count { (packageName, snapshots) ->
            to.mergeRestoredData(snapshots, from.blobs(packageName), devices).any { it != IGNORED_ROW_ID }
        }
    }
}
