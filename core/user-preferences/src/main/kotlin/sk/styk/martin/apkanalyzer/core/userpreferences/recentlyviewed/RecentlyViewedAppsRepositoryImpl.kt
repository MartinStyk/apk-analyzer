package sk.styk.martin.apkanalyzer.core.userpreferences.recentlyviewed

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import javax.inject.Inject

internal class RecentlyViewedAppsRepositoryImpl @Inject constructor(
    private val dao: RecentlyViewedAppDao,
    private val persistenceRepository: PersistenceRepository,
    private val installedAppsRepository: InstalledAppsRepository,
) : RecentlyViewedAppsRepository {

    override fun recents(): Flow<List<InstalledApp>> = persistenceRepository.observe(Key.RecentlyViewedAppsEnabled)
        .flatMapLatest { enabled ->
            if (enabled) {
                combine(
                    dao.observeRecentPackageNames(MAX_RECENTS),
                    installedAppsRepository.apps().map { apps -> apps.associateBy { it.packageName } },
                ) { recentPackages, installedApps ->
                    recentPackages.mapNotNull { installedApps[PackageName(it)] }
                }
            } else {
                flowOf(emptyList())
            }
        }

    override suspend fun addRecent(packageName: PackageName) {
        dao.recordView(packageName.value, System.currentTimeMillis())
    }

    override suspend fun hasRecents(): Boolean = persistenceRepository.get(Key.RecentlyViewedAppsEnabled) && dao.count() > 0

    private companion object {
        const val MAX_RECENTS = 8
    }
}
