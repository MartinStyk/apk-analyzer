package sk.styk.martin.apkanalyzer.manager.media

import android.content.Context
import android.media.MediaScannerConnection
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class MediaManager
@Inject
constructor(@ApplicationContext private val context: Context) {
    fun addMediaRecord(vararg files: File) {
        MediaScannerConnection.scanFile(context, files.map { it.absolutePath }.toTypedArray(), null, null)
    }

    fun removeMediaRecord(vararg files: File) {
        MediaScannerConnection.scanFile(context, files.map { it.absolutePath }.toTypedArray(), null) { path, uri ->
            context.contentResolver.delete(uri, null, null)
        }
    }
}
