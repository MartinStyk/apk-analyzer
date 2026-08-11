package sk.styk.martin.apkanalyzer.core.apps.devicefeatures

import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.LogEvent.Operation
import sk.styk.martin.apkanalyzer.core.common.logger.LogEvent.Operation.State
import sk.styk.martin.apkanalyzer.core.common.logger.LogRequest
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
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
        val request = LogRequest()
        Logger.log(TAG, Operation(OPERATION, request, State.Started))
        val systemFeatures = runCatching { packageManager.systemAvailableFeatures }
            .onFailure { Logger.log(TAG, Operation(OPERATION, request, State.Degraded, context = "reason=feature_query_failed"), it) }
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
            Logger.log(TAG, Operation(OPERATION, request, State.Succeeded, context = "feature_count=${it.featureVersions.size}"))
        }
    }
}
