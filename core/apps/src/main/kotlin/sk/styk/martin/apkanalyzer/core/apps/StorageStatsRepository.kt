package sk.styk.martin.apkanalyzer.core.apps

import kotlinx.coroutines.flow.StateFlow
import sk.styk.martin.apkanalyzer.core.common.model.AppSize

interface StorageStatsRepository {
    val isPermissionGranted: StateFlow<Boolean>
    val totalSizes: StateFlow<Map<String, AppSize>>
    fun requestTotalSizes(packageNames: List<String>)
    suspend fun queryTotalSize(packageName: String): AppSize?
}
