package sk.styk.martin.apkanalyzer.core.apps

import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

interface UsageStatsRepository {

    sealed interface DataResult<out T> {
        data object Loading : DataResult<Nothing>
        data class Available<T>(val data: T) : DataResult<T>
    }

    val isPermissionGranted: StateFlow<Boolean>
    val lastUsedTimes: StateFlow<DataResult<Map<String, Instant>>>
}
