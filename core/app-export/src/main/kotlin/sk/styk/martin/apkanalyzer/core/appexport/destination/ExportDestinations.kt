package sk.styk.martin.apkanalyzer.core.appexport.destination

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

private const val TAG = "ExportDestinations"
private const val SHARE_DIRECTORY = "inventory"

internal class ExportDestinations @Inject constructor(@ApplicationContext private val context: Context) {

    private val contentResolver = context.contentResolver

    suspend fun write(destination: Uri, write: suspend (OutputStream) -> Unit) {
        var writeCompleted = false
        try {
            val output = contentResolver.openOutputStream(destination, "w")
                ?: error("The selected destination can not be opened")
            output.use { write(it) }
            writeCompleted = true
        } finally {
            if (!writeCompleted) {
                runCatching { contentResolver.delete(destination, null, null) }
                    .onFailure { Logger.w(TAG, it, "Can not remove incomplete export") }
            }
        }
    }

    fun displayNameOr(destination: Uri, fallback: String): String {
        val displayName = runCatching {
            contentResolver.query(destination, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.onFailure {
            Logger.w(TAG, it, "Can not read the exported file name")
        }.getOrNull()
        return displayName?.takeIf { it.isNotBlank() } ?: fallback
    }

    fun createShareFile(fileName: String): Uri {
        val directory = File(context.cacheDir, SHARE_DIRECTORY)
        directory.deleteRecursively()
        check(directory.mkdirs()) { "The share directory can not be created" }
        val file = File(directory, fileName)
        check(file.createNewFile()) { "The share file can not be created" }
        return FileProvider.getUriForFile(context, context.packageName, file)
    }
}
