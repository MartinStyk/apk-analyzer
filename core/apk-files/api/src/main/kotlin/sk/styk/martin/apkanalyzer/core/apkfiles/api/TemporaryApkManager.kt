package sk.styk.martin.apkanalyzer.core.apkfiles

import android.net.Uri
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import java.io.File
import java.io.IOException

interface TemporaryApkManager {
    suspend fun copy(uri: Uri, taskId: Int): Result<File>

    fun release(apkFilePath: String): Result<Unit>
}

class ApkTooLargeException(val actualBytes: Long, val maxBytes: Long) :
    IOException("APK exceeds the maximum supported size: ${actualBytes.bytes.formatted()} (limit ${maxBytes.bytes.formatted()})")
