package sk.styk.martin.apkanalyzer.core.userpreferences.api.recentlyviewed

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apps.api.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface RecentlyViewedAppsRepository {
    fun recents(): Flow<List<InstalledApp>>
    suspend fun addRecent(packageName: PackageName)
    suspend fun hasRecents(): Boolean
}
