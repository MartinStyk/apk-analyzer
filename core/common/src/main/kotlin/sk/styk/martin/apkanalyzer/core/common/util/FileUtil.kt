package sk.styk.martin.apkanalyzer.core.common.util

import android.content.Context
import android.net.Uri
import java.io.File

object FileUtil {

    fun copyUriToCache(context: Context, uri: Uri): File? = try {
        val cacheFile = File(context.cacheDir, "temp_apk_${System.currentTimeMillis()}.apk")
        context.contentResolver.openInputStream(uri)?.use { input ->
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        cacheFile
    } catch (_: Exception) {
        null
    }
}
