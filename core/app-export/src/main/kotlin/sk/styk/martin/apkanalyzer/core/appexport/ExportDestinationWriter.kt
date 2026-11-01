package sk.styk.martin.apkanalyzer.core.appexport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.OutputStream
import javax.inject.Inject

private const val TAG = "ExportDestinationWriter"

internal class ExportDestinationWriter @Inject constructor(@ApplicationContext context: Context) {

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
}
