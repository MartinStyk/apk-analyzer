package sk.styk.martin.apkanalyzer.core.apphistory.capture

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface AppHistoryCaptureRepository {
    fun requestCapture(packageName: PackageName)

    suspend fun reconcile(packageName: PackageName): Result<Unit>
    suspend fun reconcileAll(): Result<Unit>
}
