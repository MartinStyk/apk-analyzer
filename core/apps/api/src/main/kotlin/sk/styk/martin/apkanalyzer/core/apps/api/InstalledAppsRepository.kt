package sk.styk.martin.apkanalyzer.core.apps.api

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apps.api.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface InstalledAppsRepository {
    fun apps(): Flow<List<InstalledApp>>
    suspend fun awaitFullyEnrichedApps(): List<InstalledApp>
    suspend fun app(packageName: PackageName): InstalledApp?
}
