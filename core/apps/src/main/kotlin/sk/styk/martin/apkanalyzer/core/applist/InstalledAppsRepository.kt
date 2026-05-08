package sk.styk.martin.apkanalyzer.core.applist

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.applist.model.InstalledApp

interface InstalledAppsRepository {
    fun apps(): Flow<List<InstalledApp>>
}
