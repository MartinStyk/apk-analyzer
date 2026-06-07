package sk.styk.martin.apkanalyzer.core.apps

import kotlinx.coroutines.flow.StateFlow
import sk.styk.martin.apkanalyzer.core.common.model.AppSize

interface StorageStatsRepository {
    sealed interface DataResult<out T> {
        data object Loading : DataResult<Nothing>
        data class Available<T>(val data: T) : DataResult<T>
    }

    val isPermissionGranted: StateFlow<Boolean>
    val totalSizes: StateFlow<DataResult<Map<String, AppSize>>>
    fun requestTotalSizes(packageNames: List<String>)
}
