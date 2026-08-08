package sk.styk.martin.apkanalyzer.core.common.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

object FileUtil {

    suspend fun copyApkUriToCache(
        context: Context,
        uri: Uri,
        dispatcherProvider: DispatcherProvider,
    ): Result<File> {
        var copiedFile: File? = null
        return try {
            withContext(dispatcherProvider.io()) {
                copyApkUriToCache(context, uri).also { result ->
                    copiedFile = result.getOrNull()
                }
            }
        } catch (error: CancellationException) {
            copiedFile?.delete()
            throw error
        }
    }

    private fun copyApkUriToCache(context: Context, uri: Uri): Result<File> {
        var cacheFile: File? = null
        return try {
            val destination = File.createTempFile("temp_apk_", ".apk", context.cacheDir)
            cacheFile = destination
            val input = context.contentResolver.openInputStream(uri)
                ?: throw FileNotFoundException("Content provider returned no stream")
            input.use {
                destination.outputStream().use { output ->
                    it.copyTo(output)
                }
            }
            Result.success(destination)
        } catch (error: IOException) {
            cacheFile?.delete()
            Result.failure(error)
        } catch (error: SecurityException) {
            cacheFile?.delete()
            Result.failure(error)
        } catch (error: IllegalArgumentException) {
            cacheFile?.delete()
            Result.failure(error)
        }
    }
}
