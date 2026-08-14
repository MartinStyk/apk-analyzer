package sk.styk.martin.apkanalyzer.core.userpreferences.searchhistory

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

internal class SearchHistoryRepositoryImpl @Inject constructor(
    private val dao: SearchHistoryDao,
    private val installedAppsRepository: InstalledAppsRepository,
) : SearchHistoryRepository {

    override fun history(): Flow<List<SearchHistoryEntry>> = combine(
        dao.observeRecent(MAX_HISTORY),
        installedAppsRepository.apps().map { apps -> apps.associateBy { it.packageName } },
    ) { entries, installedApps ->
        entries.map { entity ->
            val packageName = PackageName(entity.packageName)
            SearchHistoryEntry(
                packageName = packageName,
                query = entity.query,
                app = installedApps[packageName],
            )
        }
    }

    override suspend fun addSearch(packageName: PackageName, query: String) {
        dao.recordSearch(packageName.value, query, System.currentTimeMillis())
    }

    override suspend fun removeEntry(packageName: PackageName) {
        dao.deletePackage(packageName.value)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }

    private companion object {
        const val MAX_HISTORY = 15
    }
}
