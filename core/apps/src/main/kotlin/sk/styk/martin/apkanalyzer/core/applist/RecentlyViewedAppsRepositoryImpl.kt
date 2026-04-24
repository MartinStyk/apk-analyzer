package sk.styk.martin.apkanalyzer.core.applist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import sk.styk.martin.apkanalyzer.core.applist.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import javax.inject.Inject

internal class RecentlyViewedAppsRepositoryImpl @Inject constructor(private val persistenceRepository: PersistenceRepository, private val installedAppsRepository: InstalledAppsRepository) : RecentlyViewedAppsRepository {

    override fun recents(): Flow<List<InstalledApp>> = combine(
        persistenceRepository.observe(Key.RecentlyViewedApps),
        installedAppsRepository.apps().map { it.associateBy { it.packageName } },
    ) { recentPackages, installedApps ->
        recentPackages.mapNotNull { installedApps[it] }
    }

    override suspend fun addRecent(packageName: String) {
        val current = persistenceRepository.observe(Key.RecentlyViewedApps).first()
        val updated = (listOf(packageName) + current.filter { it != packageName }).take(MAX_RECENTS)
        persistenceRepository.save(Key.RecentlyViewedApps, updated)
    }

    override suspend fun hasRecents(): Boolean = persistenceRepository.observe(Key.RecentlyViewedApps).first().isNotEmpty()

    private companion object {
        const val MAX_RECENTS = 8
    }
}
