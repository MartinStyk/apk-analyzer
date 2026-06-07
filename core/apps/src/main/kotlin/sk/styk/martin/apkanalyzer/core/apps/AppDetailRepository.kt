package sk.styk.martin.apkanalyzer.core.apps

import sk.styk.martin.apkanalyzer.core.apps.model.AppDetailData
import java.io.File

interface AppDetailRepository {
    fun installedPackageDetails(packageName: String): Result<AppDetailData>
    fun apkFilePackageDetails(accessibleFile: File): Result<AppDetailData>
}
