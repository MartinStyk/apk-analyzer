package sk.styk.martin.apkanalyzer.core.applist

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.applist.model.InstalledApp

interface RecentlyViewedAppsRepository {
    fun recents(): Flow<List<InstalledApp>>
    suspend fun addRecent(packageName: String)
}

