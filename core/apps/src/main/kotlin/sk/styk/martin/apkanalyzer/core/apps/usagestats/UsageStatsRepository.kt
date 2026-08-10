package sk.styk.martin.apkanalyzer.core.apps.usagestats

import kotlinx.coroutines.flow.StateFlow
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

interface UsageStatsRepository {
    val isPermissionGranted: StateFlow<Boolean>
    val lastUsedTimes: StateFlow<Map<PackageName, Instant>>
    suspend fun queryLastUsedTime(packageName: PackageName): Instant?
}
