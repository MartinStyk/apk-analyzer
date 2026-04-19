package sk.styk.martin.apkanalyzer.manager.file

import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.IntRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.yield
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.manager.notification.NotificationManager
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkSaveManager
@Inject
constructor(private val contentResolver: ContentResolver, private val notificationManager: NotificationManager, private val dispatcherProvider: DispatcherProvider) {
    sealed class AppSaveStatus(open val outputUri: Uri) {
        data class Progress(@IntRange(from = 0, to = 100) val currentProgress: Int, override val outputUri: Uri) : AppSaveStatus(outputUri)

        data class Done(override val outputUri: Uri) : AppSaveStatus(outputUri)
    }

    fun saveApk(appName: String, sourceFile: File, targetUri: Uri): Flow<AppSaveStatus> = saveApkInternal(sourceFile, targetUri)
        .distinctUntilChanged()
        .debounce(1500)
        .catch { Logger.e("Export", it, "Saving apk failed failed. Appname=$appName, sourceFile=$sourceFile, targetUri=$targetUri") }

    private fun saveApkInternal(sourceFile: File, targetUri: Uri) = flow {
        emit(AppSaveStatus.Progress(0, targetUri))
        val fileSize = sourceFile.length()

        FileInputStream(sourceFile).use { source ->

            contentResolver.openOutputStream(targetUri)!!.use { output ->

                val buffer = ByteArray(10240)
                var len: Int = source.read(buffer)
                var readBytes = len
                while (len > 0) {
                    yield()
                    output.write(buffer, 0, len)
                    len = source.read(buffer)
                    readBytes += len
                    emit(AppSaveStatus.Progress((readBytes * 100 / fileSize).toInt(), targetUri))
                }
            }
        }

        emit(AppSaveStatus.Done(targetUri))
    }.flowOn(dispatcherProvider.io())
}
