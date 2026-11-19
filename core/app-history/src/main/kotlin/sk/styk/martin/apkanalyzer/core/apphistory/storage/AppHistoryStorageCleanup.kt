package sk.styk.martin.apkanalyzer.core.apphistory.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import sk.styk.martin.apkanalyzer.core.apphistory.capture.APP_HISTORY
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.UNCOMPRESSED_APP_HISTORY_DATABASE_NAMES
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import javax.inject.Inject

internal class AppHistoryStorageCleanup @Inject constructor(@ApplicationContext private val context: Context) {
    fun deleteUncompressedDatabases() {
        UNCOMPRESSED_APP_HISTORY_DATABASE_NAMES
            .filter { context.deleteDatabase(it) }
            .forEach { Logger.i(APP_HISTORY, "Deleted uncompressed history database $it") }
    }
}
