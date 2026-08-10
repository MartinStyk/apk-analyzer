package sk.styk.martin.apkanalyzer.core.apps.devicefeatures

import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.logger.nextOperationRequest
import sk.styk.martin.apkanalyzer.core.common.logger.operationLogMessage
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DeviceFeaturesRepositoryImpl"
private const val OPERATION = "device_features"

@Singleton
internal class DeviceFeaturesRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val dispatcherProvider: DispatcherProvider,
) : DeviceFeaturesRepository {

    private val cachedDeviceFeatures: DeviceFeatures by lazy { readDeviceFeatures() }

    override suspend fun deviceFeatures(): DeviceFeatures = withContext(dispatcherProvider.io()) { cachedDeviceFeatures }

    private fun readDeviceFeatures(): DeviceFeatures {
        val requestId = nextOperationRequest()
        Logger.d(TAG, operationLogMessage(OPERATION, requestId, event = "started"))
        val systemFeatures = runCatching { packageManager.systemAvailableFeatures }
            .onFailure { Logger.w(TAG, it, operationLogMessage(OPERATION, requestId, event = "degraded", context = "reason=feature_query_failed")) }
            .getOrNull()
            ?: return DeviceFeatures.Unknown

        return DeviceFeatures(
            featureVersions = systemFeatures
                .filter { it.name != null }
                .associate { it.name to it.version },
            openGlEsVersion = systemFeatures
                .firstOrNull { it.name == null && it.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED }
                ?.reqGlEsVersion,
        ).also {
            Logger.i(TAG, operationLogMessage(OPERATION, requestId, event = "succeeded", context = "feature_count=${it.featureVersions.size}"))
        }
    }
}
