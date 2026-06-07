package sk.styk.martin.apkanalyzer.core.apps

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp

interface InstalledAppsRepository {
    fun apps(): Flow<List<InstalledApp>>
}
