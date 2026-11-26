package sk.styk.martin.apkanalyzer.core.apps.usagestats

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

class UsageStatsRepositoryFake(lastUsedTimes: Map<PackageName, Instant> = emptyMap()) : UsageStatsRepository {
    override val isPermissionGranted: StateFlow<Boolean> = MutableStateFlow(true)
    override val canRequestPermission: Boolean = true
    override val lastUsedTimes: StateFlow<Map<PackageName, Instant>> = MutableStateFlow(lastUsedTimes)

    override suspend fun queryLastUsedTime(packageName: PackageName): Instant? = lastUsedTimes.value[packageName]

    override suspend fun ensureLoaded() = Unit
}
