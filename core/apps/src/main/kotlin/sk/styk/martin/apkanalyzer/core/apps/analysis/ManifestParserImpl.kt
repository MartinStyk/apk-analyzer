package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PackageManager
import android.content.res.Resources
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.model.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.model.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

internal class ManifestParserImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val dispatcherProvider: DispatcherProvider,
    private val manifestXmlRenderer: ManifestXmlRenderer,
    private val componentManifestParser: ComponentManifestParser,
) : ManifestParser {

    override suspend fun manifest(reference: AppReference): Result<ParsedManifest> = when (reference) {
        is AppReference.InstalledPackage -> installedPackageManifest(reference.packageName)
        is AppReference.ApkFile -> apkFileManifest(reference.path)
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
            }.onFailure {
                Logger.e(TAG, it, "Can not read component intent filters from $reference")
            }
        }

    private suspend fun installedPackageManifest(packageName: PackageName): Result<ParsedManifest> = withContext(dispatcherProvider.io()) {
        runCatchingCancellable {
            val applicationInfo = packageManager.getApplicationInfo(packageName.value, 0)
            ParsedManifest(
                xml = manifestXmlRenderer.render(resourcesForApk(applicationInfo.sourceDir)),
                additionalInstalledSplits = applicationInfo.splitSourceDirs.orEmpty().size,
            )
        }.onFailure {
            Logger.e(TAG, it, "Can not read manifest for installed package $packageName")
        }
    }

    private suspend fun apkFileManifest(apkPath: String): Result<ParsedManifest> = withContext(dispatcherProvider.io()) {
        runCatchingCancellable {
            ParsedManifest(
                xml = manifestXmlRenderer.render(resourcesForApk(apkPath)),
                additionalInstalledSplits = 0,
            )
        }.onFailure {
            Logger.e(TAG, it, "Can not read manifest from $apkPath")
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

    companion object {
        private const val TAG = "ManifestParser"
    }
}
