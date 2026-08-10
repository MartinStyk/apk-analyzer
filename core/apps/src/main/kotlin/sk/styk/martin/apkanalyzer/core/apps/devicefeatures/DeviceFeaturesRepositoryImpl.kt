package sk.styk.martin.apkanalyzer.core.apps.devicefeatures

import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.logger.nextOperationRequest
import sk.styk.martin.apkanalyzer.core.common.logger.operationLogMessage
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceAttributeName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceMetricName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTraceName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.measureStage
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "DeviceFeaturesRepositoryImpl"
private const val OPERATION = "device_features"

@Singleton
internal class DeviceFeaturesRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val dispatcherProvider: DispatcherProvider,
    private val performanceTracker: PerformanceTracker,
) : DeviceFeaturesRepository {

    private val cachedDeviceFeatures: DeviceFeatures by lazy { readDeviceFeatures() }

    override suspend fun deviceFeatures(): DeviceFeatures = withContext(dispatcherProvider.io()) { cachedDeviceFeatures }

    private fun readDeviceFeatures(): DeviceFeatures {
        val requestId = nextOperationRequest()
        val trace = performanceTracker.startTrace(PerformanceTraceName.DEVICE_FEATURES_LOAD)
        var outcome = OUTCOME_ERROR
        Logger.d(TAG, operationLogMessage(OPERATION, requestId, event = "started"))
        try {
            val systemFeatures: Array<FeatureInfo>? = try {
                trace.measureStage(PerformanceMetricName.FEATURE_QUERY_US) {
                    packageManager.systemAvailableFeatures
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                Logger.w(
                    TAG,
                    failure,
                    operationLogMessage(OPERATION, requestId, event = "degraded", context = "reason=feature_query_failed"),
                )
                trace.putMetric(PerformanceMetricName.FEATURE_COUNT, 0)
                outcome = OUTCOME_DEGRADED
                return DeviceFeatures.Unknown
            }
            if (systemFeatures == null) {
                Logger.w(TAG, operationLogMessage(OPERATION, requestId, event = "degraded", context = "reason=feature_query_unavailable"))
                trace.putMetric(PerformanceMetricName.FEATURE_COUNT, 0)
                outcome = OUTCOME_DEGRADED
                return DeviceFeatures.Unknown
            }

            val deviceFeatures = trace.measureStage(PerformanceMetricName.FEATURE_MAPPING_US) {
                DeviceFeatures(
                    featureVersions = systemFeatures
                        .filter { it.name != null }
                        .associate { it.name to it.version },
                    openGlEsVersion = systemFeatures
                        .firstOrNull { it.name == null && it.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED }
                        ?.reqGlEsVersion,
                )
            }
            trace.putMetric(PerformanceMetricName.FEATURE_COUNT, deviceFeatures.featureVersions.size.toLong())
            Logger.i(
                TAG,
                operationLogMessage(OPERATION, requestId, event = "succeeded", context = "feature_count=${deviceFeatures.featureVersions.size}"),
            )
            outcome = OUTCOME_SUCCESS
            return deviceFeatures
        } catch (cancellation: CancellationException) {
            outcome = OUTCOME_CANCELLED
            Logger.d(TAG, operationLogMessage(OPERATION, requestId, event = "cancelled"))
            throw cancellation
        } catch (failure: Throwable) {
            Logger.e(TAG, failure, operationLogMessage(OPERATION, requestId, event = "failed"))
            throw failure
        } finally {
            trace.putAttribute(PerformanceAttributeName.OUTCOME, outcome)
            trace.stop()
        }
    }
}

private const val OUTCOME_SUCCESS = "success"
private const val OUTCOME_DEGRADED = "degraded"
private const val OUTCOME_ERROR = "error"
private const val OUTCOME_CANCELLED = "cancelled"
