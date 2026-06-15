package sk.styk.martin.apkanalyzer.core.apps

import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

interface UsageStatsRepository {
    val isPermissionGranted: StateFlow<Boolean>
    val lastUsedTimes: StateFlow<Map<String, Instant>>
    suspend fun queryLastUsedTime(packageName: String): Instant?
}
