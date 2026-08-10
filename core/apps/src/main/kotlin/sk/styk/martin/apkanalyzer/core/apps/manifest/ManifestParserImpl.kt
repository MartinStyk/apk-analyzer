package sk.styk.martin.apkanalyzer.core.apps.manifest

import android.content.pm.PackageManager
import android.content.res.Resources
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.splitCountBucket
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.logger.nextOperationRequest
import sk.styk.martin.apkanalyzer.core.common.logger.operationLogMessage
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceAttributeName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceMetricName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTraceName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.measureStage
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
    private val performanceTracker: PerformanceTracker,
) : ManifestParser {

    @Suppress("SuspendFunSwallowedCancellation")
    override suspend fun manifest(reference: AppReference): Result<ParsedManifest> {
        val requestId = nextOperationRequest()
        val trace = performanceTracker.startTrace(PerformanceTraceName.MANIFEST_LOAD)
        trace.putAttribute(PerformanceAttributeName.ANALYSIS_MODE, reference.analysisMode())
        var outcome = OUTCOME_ERROR
        Logger.d(TAG, operationLogMessage(OPERATION_MANIFEST, requestId, event = "started", context = reference.diagnosticContext()))
        return try {
            val result = withContext(dispatcherProvider.io()) {
                runCatchingCancellable {
                    val manifestResources = trace.measureStage(PerformanceMetricName.RESOURCE_LOOKUP_US) {
                        manifestResources(reference)
                    }
                    trace.putMetric(PerformanceMetricName.SPLIT_COUNT, manifestResources.additionalInstalledSplits.toLong())
                    trace.putAttribute(
                        PerformanceAttributeName.SPLIT_COUNT_BUCKET,
                        splitCountBucket(manifestResources.additionalInstalledSplits),
                    )
                    val document = trace.measureStage(PerformanceMetricName.MANIFEST_PARSE_US) {
                        manifestXmlRenderer.parse(manifestResources.resources)
                    }
                    val xml = trace.measureStage(PerformanceMetricName.XML_RENDER_US) {
                        manifestXmlRenderer.render(document)
                    }
                    ParsedManifest(
                        xml = xml,
                        additionalInstalledSplits = manifestResources.additionalInstalledSplits,
                    )
                }
            }
            result.onSuccess {
                Logger.i(TAG, operationLogMessage(OPERATION_MANIFEST, requestId, event = "succeeded", context = reference.diagnosticContext()))
            }.onFailure {
                Logger.e(TAG, it, operationLogMessage(OPERATION_MANIFEST, requestId, event = "failed", context = reference.diagnosticContext()))
            }
            outcome = if (result.isSuccess) OUTCOME_SUCCESS else OUTCOME_ERROR
            result
        } catch (cancellation: CancellationException) {
            outcome = OUTCOME_CANCELLED
            Logger.d(TAG, operationLogMessage(OPERATION_MANIFEST, requestId, event = "cancelled", context = reference.diagnosticContext()))
            throw cancellation
        } finally {
            trace.putAttribute(PerformanceAttributeName.OUTCOME, outcome)
            trace.stop()
        }
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

    private fun manifestResources(reference: AppReference): ManifestResources = when (reference) {
        is AppReference.InstalledPackage -> {
            val applicationInfo = packageManager.getApplicationInfo(reference.packageName.value, 0)
            ManifestResources(
                resources = resourcesForApk(applicationInfo.sourceDir),
                additionalInstalledSplits = applicationInfo.splitSourceDirs.orEmpty().size,
            )
        }

        is AppReference.ApkFile -> ManifestResources(
            resources = resourcesForApk(reference.path),
            additionalInstalledSplits = 0,
        )
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

private data class ManifestResources(val resources: Resources, val additionalInstalledSplits: Int)

private fun AppReference.analysisMode(): String = when (this) {
    is AppReference.InstalledPackage -> "installed"
    is AppReference.ApkFile -> "apk_file"
}

private fun AppReference.diagnosticContext(): String = when (this) {
    is AppReference.InstalledPackage -> "mode=installed package=${packageName.value}"
    is AppReference.ApkFile -> "mode=apk_file apk_path=$path"
}

private const val OUTCOME_SUCCESS = "success"
private const val OUTCOME_ERROR = "error"
private const val OUTCOME_CANCELLED = "cancelled"
