package sk.styk.martin.apkanalyzer.core.common.file

import android.net.Uri
import java.io.OutputStream

interface DocumentFileManager {
    suspend fun <T> writeOrDeleteOnFailure(destination: Uri, write: suspend (OutputStream) -> T): Result<T>

    suspend fun displayNameOr(destination: Uri, fallback: String): String
}
