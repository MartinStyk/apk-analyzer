package sk.styk.martin.apkanalyzer.core.apps.signing

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import sk.styk.martin.apkanalyzer.core.apps.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.apps.appCountBucket
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.logger.nextOperationRequest
import sk.styk.martin.apkanalyzer.core.common.logger.operationLogMessage
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceAttributeName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceMetricName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTraceName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.measureStage
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "AppSigningRepositoryImpl"
private const val OPERATION = "device_signing"

internal class AppSigningRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val certificateExtractor: CertificateExtractor,
    packageChangesObserver: PackageChangesObserver,
    dispatcherProvider: DispatcherProvider,
    appScope: CoroutineScope,
    private val performanceTracker: PerformanceTracker,
) : AppSigningRepository {

    @Suppress("TooGenericExceptionCaught")
    private val cachedSigning = packageChangesObserver.observe()
        .map { AppSigningLoadTrigger.PackageChange }
        .onStart { emit(AppSigningLoadTrigger.Initial) }
        .mapLatest { trigger ->
            val requestId = nextOperationRequest()
            Logger.d(TAG, operationLogMessage(OPERATION, requestId, event = "started"))
            try {
                val result = loadAllSigning(trigger)
                Logger.i(TAG, operationLogMessage(OPERATION, requestId, event = "succeeded", context = "app_count=${result.size}"))
                result
            } catch (cancellation: CancellationException) {
                Logger.d(TAG, operationLogMessage(OPERATION, requestId, event = "cancelled"))
                throw cancellation
            } catch (failure: Throwable) {
                Logger.e(TAG, failure, operationLogMessage(OPERATION, requestId, event = "failed"))
                throw failure
            }
        }
        .flowOn(dispatcherProvider.io())
        .shareIn(appScope, SharingStarted.Lazily, replay = 1)

    override fun signing(): Flow<Map<PackageName, AppSigning>> = cachedSigning

    @SuppressLint("QueryPermissionsNeeded")
    private fun loadAllSigning(trigger: AppSigningLoadTrigger): Map<PackageName, AppSigning> {
        val trace = performanceTracker.startTrace(PerformanceTraceName.APP_SIGNING_INDEX_LOAD)
        trace.putAttribute(PerformanceAttributeName.TRIGGER, trigger.attributeValue)
        var outcome = OUTCOME_ERROR
        try {
            val packages = trace.measureStage(PerformanceMetricName.PACKAGE_QUERY_US) {
                packageManager.getInstalledPackages(PackageManager.GET_SIGNING_CERTIFICATES)
            }
            val signing = trace.measureStage(PerformanceMetricName.CERTIFICATE_MAPPING_US) {
                packages.associate { PackageName(it.packageName) to certificateExtractor.getAppSigning(it) }
            }
            val certificateCount = signing.values.sumOf { it.currentCertificates.size + it.pastCertificates.size }
            trace.putMetric(PerformanceMetricName.APP_COUNT, signing.size.toLong())
            trace.putMetric(PerformanceMetricName.CERTIFICATE_COUNT, certificateCount.toLong())
            trace.putAttribute(PerformanceAttributeName.APP_COUNT_BUCKET, appCountBucket(signing.size))
            outcome = OUTCOME_SUCCESS
            return signing
        } catch (cancellation: CancellationException) {
            outcome = OUTCOME_CANCELLED
            throw cancellation
        } finally {
            trace.putAttribute(PerformanceAttributeName.OUTCOME, outcome)
            trace.stop()
        }
    }
}

private enum class AppSigningLoadTrigger(val attributeValue: String) {
    Initial(TRIGGER_INITIAL),
    PackageChange(TRIGGER_PACKAGE_CHANGE),
}

private const val OUTCOME_SUCCESS = "success"
private const val OUTCOME_ERROR = "error"
private const val OUTCOME_CANCELLED = "cancelled"
private const val TRIGGER_INITIAL = "initial"
private const val TRIGGER_PACKAGE_CHANGE = "package_change"
