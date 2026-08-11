package sk.styk.martin.apkanalyzer.core.apps.manifest

import android.content.pm.PackageManager
import android.content.res.Resources
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.LogEvent.Operation
import sk.styk.martin.apkanalyzer.core.common.logger.LogEvent.Operation.State
import sk.styk.martin.apkanalyzer.core.common.logger.LogRequest
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

private const val TAG = "ManifestParser"
private const val OPERATION_MANIFEST = "manifest"
private const val OPERATION_COMPONENT_INTENT_FILTERS = "component_intent_filters"

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

    override suspend fun componentIntentFilters(reference: AppReference): Result<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>> {
        val request = LogRequest()
        val context = when (reference) {
            is AppReference.InstalledPackage -> "mode=installed package=${reference.packageName.value}"
            is AppReference.ApkFile -> "mode=apk_file apk_path=${reference.path}"
        }
        Logger.log(TAG, Operation(OPERATION_COMPONENT_INTENT_FILTERS, request, State.Started))
        return withContext(dispatcherProvider.io()) {
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
        }.also { result ->
            val state = if (result.isSuccess) State.Succeeded else State.Degraded
            val resultContext = result.getOrNull()?.let { "count=${it.size}" } ?: context
            Logger.log(TAG, Operation(OPERATION_COMPONENT_INTENT_FILTERS, request, state, context = resultContext), result.exceptionOrNull())
        }
    }

    private suspend fun installedPackageManifest(packageName: PackageName): Result<ParsedManifest> {
        val request = LogRequest()
        val context = "mode=installed package=${packageName.value}"
        Logger.log(TAG, Operation(OPERATION_MANIFEST, request, State.Started, context = context))
        return withContext(dispatcherProvider.io()) {
            runCatchingCancellable {
                val applicationInfo = packageManager.getApplicationInfo(packageName.value, 0)
                ParsedManifest(
                    xml = manifestXmlRenderer.render(resourcesForApk(applicationInfo.sourceDir)),
                    additionalInstalledSplits = applicationInfo.splitSourceDirs.orEmpty().size,
                )
            }
        }.also { result ->
            val state = if (result.isSuccess) State.Succeeded else State.Failed
            Logger.log(TAG, Operation(OPERATION_MANIFEST, request, state, context = context), result.exceptionOrNull())
        }
    }

    private suspend fun apkFileManifest(apkPath: String): Result<ParsedManifest> {
        val request = LogRequest()
        val context = "mode=apk_file apk_path=$apkPath"
        Logger.log(TAG, Operation(OPERATION_MANIFEST, request, State.Started, context = context))
        return withContext(dispatcherProvider.io()) {
            runCatchingCancellable {
                ParsedManifest(
                    xml = manifestXmlRenderer.render(resourcesForApk(apkPath)),
                    additionalInstalledSplits = 0,
                )
            }
        }.also { result ->
            val state = if (result.isSuccess) State.Succeeded else State.Failed
            Logger.log(TAG, Operation(OPERATION_MANIFEST, request, state, context = context), result.exceptionOrNull())
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
