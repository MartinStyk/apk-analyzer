package sk.styk.martin.apkanalyzer.core.appexport.appdata

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import sk.styk.martin.apkanalyzer.core.appexport.InventoryDataSource
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigningRepository
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import javax.inject.Inject

internal class InventoryAppDataLoader @Inject constructor(
    private val installedAppsRepository: InstalledAppsRepository,
    private val appDetailRepository: AppDetailRepository,
    private val appSigningRepository: AppSigningRepository,
) {

    fun load(apps: List<InstalledApp>, attributes: List<InventoryAttribute>): Flow<InventoryAppData> = flow {
        val sources = attributes.map { it.source }.toSet()
        val resolvedApps = if (InventoryDataSource.UsageStats in sources) apps.withUsageStats() else apps
        val signing = if (InventoryDataSource.SigningIndex in sources) appSigningRepository.signing().first() else emptyMap()
        val needsAnalysis = InventoryDataSource.AppAnalysis in sources
        resolvedApps.forEach { app ->
            val detail = if (needsAnalysis) appDetailRepository.details(AppReference.InstalledPackage(app.packageName)) else null
            emit(
                InventoryAppData(
                    app = app,
                    signing = signing[app.packageName],
                    detail = detail?.getOrNull(),
                    isComplete = detail?.isFailure != true,
                ),
            )
        }
    }

    private suspend fun List<InstalledApp>.withUsageStats(): List<InstalledApp> {
        val enrichedApps = installedAppsRepository.awaitFullyEnrichedApps().associateBy { it.packageName }
        return map { enrichedApps[it.packageName] ?: it }
    }
}
