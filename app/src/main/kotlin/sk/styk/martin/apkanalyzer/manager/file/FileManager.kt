package sk.styk.martin.apkanalyzer.manager.file

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.PrintWriter
import javax.inject.Inject

class FileManager
@Inject
constructor(@ApplicationContext private val context: Context, private val contentResolver: ContentResolver, private val dispatcherProvider: DispatcherProvider) {
    private val cacheDirectory by lazy { context.cacheDir }

    @Throws(IOException::class)
    suspend fun createTempFileFromUri(uri: Uri, fileName: String): File = withContext(dispatcherProvider.io()) {
        val tempFile = File(cacheDirectory, fileName)
        contentResolver.openInputStream(uri).use { inputStream ->
            if (inputStream != null) {
                copy(inputStream, tempFile)
            }
            tempFile
        }
    }

    @Throws(IOException::class)
    fun deleteTempFile(fileName: String) {
        val tempFile = File(cacheDirectory, fileName)
        tempFile.delete()
    }

    @Throws(IOException::class)
    fun writeString(content: String, targetFileUri: Uri) {
        PrintWriter(contentResolver.openOutputStream(targetFileUri)!!).use {
            it.println(content)
        }
    }

    @Throws(IOException::class)
    suspend fun copy(src: InputStream, dst: File) {
        FileOutputStream(dst).use { output ->

            val buffer = ByteArray(10240)
            var len: Int = src.read(buffer)
            var readBytes = len
            while (len > 0) {
                yield()
                output.write(buffer, 0, len)
                len = src.read(buffer)
                readBytes += len
            }
        }
        delay(500)
    }
}
