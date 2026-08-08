package sk.styk.martin.apkanalyzer.core.apkfiles

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

internal class TemporaryApkManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityManager: ActivityManager,
    private val dispatcherProvider: DispatcherProvider,
) : TemporaryApkManager {

    private val cacheRoot = File(context.cacheDir, CACHE_DIRECTORY)

    override suspend fun copy(uri: Uri, taskId: Int): Result<File> {
        var copiedFile: File? = null
        return try {
            withContext(dispatcherProvider.io()) {
                copyToTaskDirectory(uri, taskId).also { result ->
                    copiedFile = result.getOrNull()
                }
            }
        } catch (error: CancellationException) {
            copiedFile?.let(::releaseAfterCopyFailure)
            throw error
        }
    }

    override fun release(apkFilePath: String): Result<Unit> = runCatching {
        val file = File(apkFilePath).canonicalFile
        val taskDirectory = file.parentFile
        require(taskDirectory?.parentFile == cacheRoot.canonicalFile) {
            "Temporary APK is outside the managed cache directory"
        }
        releaseFile(file)
    }

    private suspend fun copyToTaskDirectory(uri: Uri, taskId: Int): Result<File> {
        var destination: File? = null
        return try {
            cleanupDiscardedTasks()
            cleanupLegacyFiles()
            val taskDirectory = File(cacheRoot, "$TASK_DIRECTORY_PREFIX$taskId")
            if (!taskDirectory.isDirectory && !taskDirectory.mkdirs()) {
                throw IOException("Unable to create temporary APK directory")
            }
            destination = File.createTempFile(FILE_PREFIX, FILE_SUFFIX, taskDirectory)
            val input = context.contentResolver.openInputStream(uri)
                ?: throw FileNotFoundException("Content provider returned no stream")
            input.use {
                destination.outputStream().use { output ->
                    it.copyTo(output, MAX_APK_SIZE_BYTES)
                }
            }
            Result.success(destination)
        } catch (error: CancellationException) {
            destination?.let(::releaseAfterCopyFailure)
            throw error
        } catch (error: IOException) {
            destination?.let(::releaseAfterCopyFailure)
            Result.failure(error)
        } catch (error: SecurityException) {
            destination?.let(::releaseAfterCopyFailure)
            Result.failure(error)
        } catch (error: IllegalArgumentException) {
            destination?.let(::releaseAfterCopyFailure)
            Result.failure(error)
        }
    }

    @Suppress("DEPRECATION")
    private fun cleanupDiscardedTasks() {
        val activeTaskIds = activityManager.appTasks
            .mapNotNull { appTask -> appTask.taskInfo?.persistentId }
            .toSet()
        cacheRoot.listFiles()
            ?.filter(File::isDirectory)
            ?.filter { directory ->
                val taskId = directory.name.removePrefix(TASK_DIRECTORY_PREFIX).toIntOrNull()
                taskId == null || taskId !in activeTaskIds
            }
            ?.forEach(::deleteDirectory)
    }

    private fun cleanupLegacyFiles() {
        cacheRoot.parentFile
            ?.listFiles { file -> file.isFile && file.name.startsWith(LEGACY_FILE_PREFIX) }
            ?.forEach { file ->
                if (!file.delete()) {
                    Logger.e(TAG, IOException("Unable to delete ${file.absolutePath}"), "Unable to clean legacy temporary APK")
                }
            }
    }

    private fun releaseFile(file: File) {
        if (file.exists() && !file.delete()) {
            throw IOException("Unable to delete ${file.absolutePath}")
        }

        val taskDirectory = file.parentFile
        if (taskDirectory?.list()?.isEmpty() == true && !taskDirectory.delete()) {
            throw IOException("Unable to delete ${taskDirectory.absolutePath}")
        }
    }

    private fun releaseAfterCopyFailure(file: File) {
        runCatching { releaseFile(file) }
            .onFailure { error -> Logger.e(TAG, error, "Unable to release failed temporary APK copy") }
    }

    private fun deleteDirectory(directory: File) {
        if (!directory.deleteRecursively()) {
            Logger.e(TAG, IOException("Unable to delete ${directory.absolutePath}"), "Unable to clean discarded task APKs")
        }
    }

    private suspend fun InputStream.copyTo(output: OutputStream, maxBytes: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copiedBytes = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val bytesRead = read(buffer)
            if (bytesRead < 0) return
            copiedBytes += bytesRead
            if (copiedBytes > maxBytes) {
                throw IOException("APK exceeds the maximum supported size")
            }
            output.write(buffer, 0, bytesRead)
        }
    }

    private companion object {
        const val TAG = "TemporaryApkManager"
        const val CACHE_DIRECTORY = "apk-analysis"
        const val TASK_DIRECTORY_PREFIX = "task_"
        const val FILE_PREFIX = "apk_"
        const val FILE_SUFFIX = ".apk"
        const val LEGACY_FILE_PREFIX = "temp_apk_"
        const val MAX_APK_SIZE_BYTES = 1024L * 1024L * 1024L
    }
}
