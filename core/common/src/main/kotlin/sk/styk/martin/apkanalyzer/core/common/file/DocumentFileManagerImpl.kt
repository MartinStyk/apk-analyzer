package sk.styk.martin.apkanalyzer.core.common.file

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.OutputStream
import javax.inject.Inject

private const val TAG = "DocumentFileManager"

internal class DocumentFileManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) : DocumentFileManager {

    private val contentResolver = context.contentResolver

    override suspend fun <T> writeOrDeleteOnFailure(destination: Uri, write: suspend (OutputStream) -> T): Result<T> = withContext(dispatcherProvider.io()) {
        runCatchingCancellable {
            var isComplete = false
            try {
                val output = contentResolver.openOutputStream(destination, "wt")
                    ?: error("The selected destination can not be opened")
                val written = output.use { write(it) }
                isComplete = true
                written
            } finally {
                if (!isComplete) discardIncomplete(destination)
            }
        }
    }

    private fun discardIncomplete(destination: Uri) {
        runCatching { contentResolver.openOutputStream(destination, "wt")?.close() }
            .onFailure { Logger.w(TAG, it, "Can not empty incomplete document") }
        runCatching {
            if (DocumentsContract.isDocumentUri(context, destination)) {
                DocumentsContract.deleteDocument(contentResolver, destination)
            } else {
                contentResolver.delete(destination, null, null)
            }
        }.onFailure { Logger.w(TAG, it, "Can not remove incomplete document") }
    }

    override suspend fun displayNameOr(destination: Uri, fallback: String): String = withContext(dispatcherProvider.io()) {
        val displayName = runCatchingCancellable {
            contentResolver.query(destination, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.onFailure {
            Logger.w(TAG, it, "Can not read the document file name")
        }.getOrNull()
        displayName?.takeIf { it.isNotBlank() } ?: fallback
    }
}
