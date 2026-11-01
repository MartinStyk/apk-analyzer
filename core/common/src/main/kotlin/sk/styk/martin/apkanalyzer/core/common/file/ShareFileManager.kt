package sk.styk.martin.apkanalyzer.core.common.file

import android.net.Uri

interface ShareFileManager {
    suspend fun createShareFile(directory: String, fileName: String): Result<Uri>
}
