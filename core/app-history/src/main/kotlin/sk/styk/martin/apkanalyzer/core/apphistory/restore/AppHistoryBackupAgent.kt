package sk.styk.martin.apkanalyzer.core.apphistory.restore

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.os.ParcelFileDescriptor
import sk.styk.martin.apkanalyzer.core.apphistory.capture.APP_HISTORY
import sk.styk.martin.apkanalyzer.core.apphistory.storage.APP_HISTORY_DATABASE_NAME
import sk.styk.martin.apkanalyzer.core.apphistory.storage.APP_HISTORY_STAGING_DATABASE_NAME
import sk.styk.martin.apkanalyzer.core.common.io.limited
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class AppHistoryBackupAgent : BackupAgent() {

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?,
    ) = Unit

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?,
    ) = Unit

    override fun onRestoreFile(
        data: ParcelFileDescriptor,
        size: Long,
        destination: File,
        type: Int,
        mode: Long,
        mtime: Long,
    ) {
        val stagingName = stagingFileName(destination.name)
        if (stagingName == null) {
            super.onRestoreFile(data, size, destination, type, mode, mtime)
            return
        }

        val staging = File(destination.parentFile, stagingName)
        val input = FileInputStream(data.fileDescriptor).limited(size)
        runCatching {
            val copied = FileOutputStream(staging).use { output -> input.copyTo(output) }
            check(copied == size) { "Restored file was truncated: expected $size bytes, got $copied" }
        }
            .onSuccess { Logger.i(APP_HISTORY, "Staged restored file ${destination.name} -> $stagingName") }
            .onFailure {
                Logger.w(APP_HISTORY, it, "Failed to stage restored file ${destination.name}")
                runCatching { input.drainRemaining() }
                    .onFailure { drainError -> Logger.w(APP_HISTORY, drainError, "Failed to drain restore pipe after staging failure") }
            }
    }

    private fun stagingFileName(originalName: String): String? = when (originalName) {
        APP_HISTORY_DATABASE_NAME -> APP_HISTORY_STAGING_DATABASE_NAME
        "$APP_HISTORY_DATABASE_NAME-wal" -> "$APP_HISTORY_STAGING_DATABASE_NAME-wal"
        "$APP_HISTORY_DATABASE_NAME-shm" -> "$APP_HISTORY_STAGING_DATABASE_NAME-shm"
        else -> null
    }
}
