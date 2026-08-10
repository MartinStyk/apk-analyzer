package sk.styk.martin.apkanalyzer.core.apps.manifest

import android.content.pm.PackageManager
import android.content.res.Resources
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.logger.nextOperationRequest
import sk.styk.martin.apkanalyzer.core.common.logger.operationLogMessage
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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

    @Suppress("SuspendFunSwallowedCancellation")
    override suspend fun componentIntentFilters(reference: AppReference): Result<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>> {
        val requestId = nextOperationRequest()
        Logger.d(TAG, operationLogMessage(OPERATION_COMPONENT_INTENT_FILTERS, requestId, event = "started"))
        return try {
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
            }.onSuccess {
                Logger.d(TAG, operationLogMessage(OPERATION_COMPONENT_INTENT_FILTERS, requestId, event = "succeeded", context = "count=${it.size}"))
            }.onFailure {
                Logger.w(
                    TAG,
                    it,
                    operationLogMessage(OPERATION_COMPONENT_INTENT_FILTERS, requestId, event = "degraded", context = reference.diagnosticContext()),
                )
            }
        } catch (cancellation: CancellationException) {
            Logger.d(TAG, operationLogMessage(OPERATION_COMPONENT_INTENT_FILTERS, requestId, event = "cancelled"))
            throw cancellation
        }
    }

    @Suppress("SuspendFunSwallowedCancellation")
    private suspend fun installedPackageManifest(packageName: PackageName): Result<ParsedManifest> {
        val requestId = nextOperationRequest()
        Logger.d(TAG, operationLogMessage(OPERATION_MANIFEST, requestId, event = "started", context = "mode=installed package=${packageName.value}"))
        return try {
            withContext(dispatcherProvider.io()) {
                runCatchingCancellable {
                    val applicationInfo = packageManager.getApplicationInfo(packageName.value, 0)
                    ParsedManifest(
                        xml = manifestXmlRenderer.render(resourcesForApk(applicationInfo.sourceDir)),
                        additionalInstalledSplits = applicationInfo.splitSourceDirs.orEmpty().size,
                    )
                }
            }.onSuccess {
                Logger.i(TAG, operationLogMessage(OPERATION_MANIFEST, requestId, event = "succeeded", context = "mode=installed package=${packageName.value}"))
            }.onFailure {
                Logger.e(TAG, it, operationLogMessage(OPERATION_MANIFEST, requestId, event = "failed", context = "mode=installed package=${packageName.value}"))
            }
        } catch (cancellation: CancellationException) {
            Logger.d(TAG, operationLogMessage(OPERATION_MANIFEST, requestId, event = "cancelled", context = "mode=installed package=${packageName.value}"))
            throw cancellation
        }
    }

    @Suppress("SuspendFunSwallowedCancellation")
    private suspend fun apkFileManifest(apkPath: String): Result<ParsedManifest> {
        val requestId = nextOperationRequest()
        Logger.d(TAG, operationLogMessage(OPERATION_MANIFEST, requestId, event = "started", context = "mode=apk_file apk_path=$apkPath"))
        return try {
            withContext(dispatcherProvider.io()) {
                runCatchingCancellable {
                    ParsedManifest(
                        xml = manifestXmlRenderer.render(resourcesForApk(apkPath)),
                        additionalInstalledSplits = 0,
                    )
                }
            }.onSuccess {
                Logger.i(TAG, operationLogMessage(OPERATION_MANIFEST, requestId, event = "succeeded", context = "mode=apk_file apk_path=$apkPath"))
            }.onFailure {
                Logger.e(TAG, it, operationLogMessage(OPERATION_MANIFEST, requestId, event = "failed", context = "mode=apk_file apk_path=$apkPath"))
            }
        } catch (cancellation: CancellationException) {
            Logger.d(TAG, operationLogMessage(OPERATION_MANIFEST, requestId, event = "cancelled", context = "mode=apk_file apk_path=$apkPath"))
            throw cancellation
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

private fun AppReference.diagnosticContext(): String = when (this) {
    is AppReference.InstalledPackage -> "mode=installed package=${packageName.value}"
    is AppReference.ApkFile -> "mode=apk_file apk_path=$path"
}
