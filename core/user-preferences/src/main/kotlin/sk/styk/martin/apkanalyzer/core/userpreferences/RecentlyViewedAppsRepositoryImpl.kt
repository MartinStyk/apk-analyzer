package sk.styk.martin.apkanalyzer.core.userpreferences

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
    private val persistenceRepository: PersistenceRepository,
    private val installedAppsRepository: InstalledAppsRepository,
) : RecentlyViewedAppsRepository {

    override fun recents(): Flow<List<InstalledApp>> = persistenceRepository.observe(Key.RecentlyViewedAppsEnabled)
        .flatMapLatest { enabled ->
            if (enabled) {
                combine(
                    persistenceRepository.observe(Key.RecentlyViewedApps),
                    installedAppsRepository.apps().map { apps -> apps.associateBy { it.packageName } },
                ) { recentPackages, installedApps ->
                    recentPackages.mapNotNull { installedApps[PackageName(it)] }
                }
            } else {
                flowOf(emptyList())
            }
        }

    override suspend fun addRecent(packageName: PackageName) {
        val current = persistenceRepository.get(Key.RecentlyViewedApps)
        val updated = (listOf(packageName.value) + current.filter { it != packageName.value }).take(MAX_RECENTS)
        persistenceRepository.save(Key.RecentlyViewedApps, updated)
    }

    override suspend fun hasRecents(): Boolean =
        persistenceRepository.get(Key.RecentlyViewedAppsEnabled) && persistenceRepository.get(Key.RecentlyViewedApps).isNotEmpty()

    private companion object {
        const val MAX_RECENTS = 8
    }
}
