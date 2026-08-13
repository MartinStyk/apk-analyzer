package sk.styk.martin.apkanalyzer.core.apps.manifest

import android.content.pm.PackageManager
import android.content.res.Resources
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.analysisModeAttribute
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.TraceOutcome
import sk.styk.martin.apkanalyzer.core.common.performance.outcome
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import javax.inject.Inject

private const val TAG = "ManifestParser"

internal class ManifestParserImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val dispatcherProvider: DispatcherProvider,
    private val manifestXmlRenderer: ManifestXmlRenderer,
    private val componentManifestParser: ComponentManifestParser,
    private val performanceTracker: PerformanceTracker,
) : ManifestParser {

    override suspend fun manifest(reference: AppReference): Result<ParsedManifest> = performanceTracker.startCancellableTrace("manifest_load") { trace ->
        trace["analysis_mode"] = reference.analysisModeAttribute
        val result = when (reference) {
            is AppReference.InstalledPackage -> installedPackageManifest(reference.packageName)
            is AppReference.ApkFile -> apkFileManifest(reference.path)
        }
        trace.outcome = if (result.isSuccess) TraceOutcome.Success else TraceOutcome.Error
        result
    }

    override suspend fun componentIntentFilters(reference: AppReference): Result<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>> =
        withContext(dispatcherProvider.io()) {
            runCatchingCancellable {
                when (reference) {
                    is AppReference.InstalledPackage -> installedComponentIntentFilters(reference.packageName)
                    is AppReference.ApkFile -> componentManifestParser.parse(resourcesForApk(reference.path))
                }
                    .groupBy(
                        keySelector = { it.first },
                        valueTransform = { it.second },
                    )
                    .mapValues { (_, filters) -> filters.distinct() }
            }
        }

    private suspend fun installedPackageManifest(packageName: PackageName): Result<ParsedManifest> {
        val context = "mode=installed package=${packageName.value}"
        Logger.d(TAG, "Manifest loading started: $context")
        return withContext(dispatcherProvider.io()) {
            runCatchingCancellable {
                val applicationInfo = packageManager.getApplicationInfo(packageName.value, 0)
                ParsedManifest(
                    xml = manifestXmlRenderer.render(resourcesForApk(applicationInfo.sourceDir)),
                    additionalInstalledSplits = applicationInfo.splitSourceDirs.orEmpty().size,
                )
            }
        }.onSuccess {
            Logger.i(TAG, "Manifest loading finished: $context")
        }.onFailure {
            Logger.e(TAG, it, "Manifest loading failed: $context")
        }
    }

    private suspend fun apkFileManifest(apkPath: String): Result<ParsedManifest> {
        val context = "mode=apk_file apk_path=$apkPath"
        Logger.d(TAG, "Manifest loading started: $context")
        return withContext(dispatcherProvider.io()) {
            runCatchingCancellable {
                ParsedManifest(
                    xml = manifestXmlRenderer.render(resourcesForApk(apkPath)),
                    additionalInstalledSplits = 0,
                )
            }
        }.onSuccess {
            Logger.i(TAG, "Manifest loading finished: $context")
        }.onFailure {
            Logger.e(TAG, it, "Manifest loading failed: $context")
        }
    }

    private fun installedComponentIntentFilters(packageName: PackageName): List<Pair<ComponentIntentFilterKey, ComponentIntentFilter>> {
        val applicationInfo = packageManager.getApplicationInfo(packageName.value, 0)
        val resources = packageManager.getResourcesForApplication(applicationInfo)
        return componentManifestParser.parseInstalled(
            resources = resources,
            packageName = packageName,
            expectedManifestCount = applicationInfo.splitSourceDirs.orEmpty().size + 1,
        )
    }

    private fun resourcesForApk(apkPath: String): Resources {
        val applicationInfo = packageManager.getPackageArchiveInfo(apkPath, 0)
            ?.applicationInfo
            ?: error("Can not read package information from $apkPath")
        applicationInfo.sourceDir = apkPath
        applicationInfo.publicSourceDir = apkPath
        applicationInfo.splitSourceDirs = null
        applicationInfo.splitPublicSourceDirs = null
        return packageManager.getResourcesForApplication(applicationInfo)
    }
}
