package sk.styk.martin.apkanalyzer.core.applist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import sk.styk.martin.apkanalyzer.core.applist.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import javax.inject.Inject

internal class RecentlyViewedAppsRepositoryImpl @Inject constructor(private val persistenceRepository: PersistenceRepository, private val installedAppsRepository: InstalledAppsRepository) : RecentlyViewedAppsRepository {

    override fun recents(): Flow<List<InstalledApp>> = persistenceRepository.observe(Key.RecentlyViewedAppsEnabled)
        .flatMapLatest { enabled ->
            if (enabled) {
                combine(
                    persistenceRepository.observe(Key.RecentlyViewedApps),
                    installedAppsRepository.apps().map { it.associateBy { it.packageName } },
                ) { recentPackages, installedApps ->
                    recentPackages.mapNotNull { installedApps[it] }
                }
            } else {
                flowOf(emptyList())
            }
        }

    override suspend fun addRecent(packageName: String) {
        val current = persistenceRepository.get(Key.RecentlyViewedApps)
        val updated = (listOf(packageName) + current.filter { it != packageName }).take(MAX_RECENTS)
        persistenceRepository.save(Key.RecentlyViewedApps, updated)
    }

    override suspend fun hasRecents(): Boolean = persistenceRepository.get(Key.RecentlyViewedAppsEnabled) && persistenceRepository.get(Key.RecentlyViewedApps).isNotEmpty()

    private companion object {
        const val MAX_RECENTS = 8
    }
}
