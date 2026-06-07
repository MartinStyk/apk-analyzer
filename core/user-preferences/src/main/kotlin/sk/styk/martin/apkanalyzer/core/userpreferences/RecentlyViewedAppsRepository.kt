package sk.styk.martin.apkanalyzer.core.userpreferences

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp

interface RecentlyViewedAppsRepository {
    fun recents(): Flow<List<InstalledApp>>
    suspend fun addRecent(packageName: String)
    suspend fun hasRecents(): Boolean
}
