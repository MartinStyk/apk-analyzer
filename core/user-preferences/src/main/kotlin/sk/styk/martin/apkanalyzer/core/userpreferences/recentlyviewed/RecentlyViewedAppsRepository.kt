package sk.styk.martin.apkanalyzer.core.userpreferences.recentlyviewed

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface RecentlyViewedAppsRepository {
    fun recents(): Flow<List<InstalledApp>>
    suspend fun addRecent(packageName: PackageName)
    suspend fun hasRecents(): Boolean
}
