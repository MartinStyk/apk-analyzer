package sk.styk.martin.apkanalyzer.core.apphistory.capture

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

internal interface AppHistoryCaptureRepository {
    suspend fun reconcile(packageName: PackageName): Result<Unit>
    suspend fun reconcileAll(): Result<Unit>
}
