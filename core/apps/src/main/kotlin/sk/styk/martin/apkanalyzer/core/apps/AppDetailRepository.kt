package sk.styk.martin.apkanalyzer.core.apps

import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import java.io.File

interface AppDetailRepository {
    fun installedPackageDetails(packageName: String): Result<AppDetail>
    fun apkFilePackageDetails(accessibleFile: File): Result<AppDetail>
}
