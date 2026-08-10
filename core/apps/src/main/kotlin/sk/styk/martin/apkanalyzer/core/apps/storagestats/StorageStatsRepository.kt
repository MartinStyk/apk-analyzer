package sk.styk.martin.apkanalyzer.core.apps.storagestats

import kotlinx.coroutines.flow.StateFlow
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface StorageStatsRepository {
    val isPermissionGranted: StateFlow<Boolean>
    val totalSizes: StateFlow<Map<PackageName, AppSize>>
    fun requestTotalSizes(packageNames: List<PackageName>)
    suspend fun queryTotalSize(packageName: PackageName): AppSize?
}
