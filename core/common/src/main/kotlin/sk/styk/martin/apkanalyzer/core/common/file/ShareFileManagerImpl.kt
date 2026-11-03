package sk.styk.martin.apkanalyzer.core.common.file

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.File
import javax.inject.Inject

private const val TAG = "ShareFileManager"

internal class ShareFileManagerImpl @Inject constructor(@ApplicationContext private val context: Context, private val dispatcherProvider: DispatcherProvider) :
    ShareFileManager {

    override suspend fun createShareFile(directory: String, fileName: String): Result<Uri> = withContext(dispatcherProvider.io()) {
        runCatchingCancellable {
            val shareDirectory = File(context.cacheDir, directory)
            shareDirectory.deleteRecursively()
            check(shareDirectory.mkdirs()) { "The share directory can not be created" }
            val file = File(shareDirectory, fileName)
            check(file.createNewFile()) { "The share file can not be created" }
            FileProvider.getUriForFile(context, context.packageName, file)
        }.onFailure {
            Logger.e(TAG, it, "Can not create a share file")
        }
    }
}
