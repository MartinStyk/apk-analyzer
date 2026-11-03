package sk.styk.martin.apkanalyzer.core.appexport.source

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.intentfilters.IntentFiltersRepository
import sk.styk.martin.apkanalyzer.core.apps.manifest.ManifestParser
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibrariesRepository
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigning
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigningRepository
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeRepository
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

internal class InventoryAppDataLoader @Inject constructor(
    private val installedAppsRepository: InstalledAppsRepository,
    private val appDetailRepository: AppDetailRepository,
    private val appSigningRepository: AppSigningRepository,
    private val nativeLibrariesRepository: NativeLibrariesRepository,
    private val signingSchemeRepository: SigningSchemeRepository,
    private val intentFiltersRepository: IntentFiltersRepository,
    private val manifestParser: ManifestParser,
) {

    fun load(apps: List<InstalledApp>, sources: Set<InventoryDataSource>): Flow<InventoryAppData> = flow {
        val resolvedApps = if (InventoryDataSource.UsageStats in sources) apps.withUsageStats() else apps
        val signingIndex = if (InventoryDataSource.SigningIndex in sources) appSigningRepository.signing().first() else null
        resolvedApps.forEach { app ->
            val reference = AppReference.InstalledPackage(app.packageName)
            emit(
                InventoryAppData(
                    app = app,
                    signing = signingIndex.resultFor(app.packageName),
                    detail = sourceResult(InventoryDataSource.AppAnalysis in sources) { appDetailRepository.details(reference).toSourceResult() },
                    nativeLibraries = sourceResult(InventoryDataSource.NativeLibraries in sources) {
                        nativeLibrariesRepository.nativeLibraries(reference).toSourceResult()
                    },
                    signingSchemes = sourceResult(InventoryDataSource.SigningSchemes in sources) {
                        signingSchemeRepository.signingSchemeVersions(reference).toAvailableSourceResult()
                    },
                    intentFilters = sourceResult(InventoryDataSource.IntentFilters in sources) {
                        intentFiltersRepository.componentIntentFilters(reference).toSourceResult()
                    },
                    manifest = sourceResult(InventoryDataSource.Manifest in sources) { manifestParser.manifest(reference).toSourceResult() },
                ),
            )
        }
    }

    private suspend fun <T> sourceResult(isRequested: Boolean, load: suspend () -> SourceResult<T>): SourceResult<T> =
        if (isRequested) load() else SourceResult.NotRequested

    private fun Map<PackageName, AppSigning>?.resultFor(packageName: PackageName): SourceResult<AppSigning> {
        if (this == null) return SourceResult.NotRequested
        return this[packageName]?.let { SourceResult.Loaded(it) } ?: SourceResult.Failed
    }

    private suspend fun List<InstalledApp>.withUsageStats(): List<InstalledApp> {
        val enrichedApps = installedAppsRepository.awaitFullyEnrichedApps().associateBy { it.packageName }
        return map { enrichedApps[it.packageName] ?: it }
    }
}
