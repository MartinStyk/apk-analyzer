package sk.styk.martin.apkanalyzer.core.apkfiles

import android.net.Uri
import java.io.File

interface TemporaryApkManager {
    suspend fun copy(uri: Uri, taskId: Int): Result<File>

    fun release(apkFilePath: String): Result<Unit>
}
