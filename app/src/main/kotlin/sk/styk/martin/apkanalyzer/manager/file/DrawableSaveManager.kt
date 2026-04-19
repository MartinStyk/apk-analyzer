package sk.styk.martin.apkanalyzer.manager.file

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.manager.media.MediaManager
import sk.styk.martin.apkanalyzer.manager.permission.hasScopedStorage
import sk.styk.martin.apkanalyzer.util.file.toBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject

private const val SUBDIRECTORY = "ApkAnalyzer"

class DrawableSaveManager
@Inject
constructor(private val contentResolver: ContentResolver, private val mediaManager: MediaManager, private val dispatcherProvider: DispatcherProvider) {
    suspend fun saveDrawable(drawable: Drawable, fileName: String, mimeType: String, directory: String = Environment.DIRECTORY_PICTURES, mediaContentUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI): Uri =
        withContext(dispatcherProvider.io()) {
            val bitmap = drawable.toBitmap()

            return@withContext if (hasScopedStorage()) {
                val values =
                    ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                contentResolver.run {
                    val uri = contentResolver.insert(mediaContentUri, values)!!
                    val outputStream = openOutputStream(uri)!!
                    save(outputStream, bitmap)
                    return@run uri
                }
            } else {
                val imagePath = Environment.getExternalStoragePublicDirectory(directory).absolutePath
                val subdirectory = File(imagePath, SUBDIRECTORY)
                if (!subdirectory.exists()) {
                    subdirectory.mkdirs()
                }
                val imageFile = File(imagePath, "$SUBDIRECTORY/$fileName")
                save(FileOutputStream(imageFile), bitmap)
                mediaManager.addMediaRecord(imageFile)
                imageFile.toUri()
            }
        }

    private fun save(stream: OutputStream, bitmap: Bitmap) = stream.use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
}
