package sk.styk.martin.apkanalyzer.core.apps.export

import android.net.Uri
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

data class ApkExportResult(val displayName: String, val baseApkOnly: Boolean)

data class IconExportResult(val displayName: String)

interface AppExportManager {
    suspend fun exportApk(reference: AppReference, destination: Uri): Result<ApkExportResult>
    suspend fun exportIcon(reference: AppReference, destination: Uri): Result<IconExportResult>
}
