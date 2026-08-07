package sk.styk.martin.apkanalyzer.core.apps

import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.model.DeviceFeatures
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DeviceFeaturesRepositoryImpl"

@Singleton
internal class DeviceFeaturesRepositoryImpl @Inject constructor(private val packageManager: PackageManager, private val dispatcherProvider: DispatcherProvider) : DeviceFeaturesRepository {

    private val cachedDeviceFeatures: DeviceFeatures by lazy { readDeviceFeatures() }

    override suspend fun deviceFeatures(): DeviceFeatures = withContext(dispatcherProvider.io()) { cachedDeviceFeatures }

    private fun readDeviceFeatures(): DeviceFeatures {
        val systemFeatures = runCatching { packageManager.systemAvailableFeatures }
            .onFailure { Logger.e(TAG, it, "Can not read device features") }
            .getOrNull()
            ?: return DeviceFeatures.Unknown

        return DeviceFeatures(
            featureVersions = systemFeatures
                .filter { it.name != null }
                .associate { it.name to it.version },
            openGlEsVersion = systemFeatures
                .firstOrNull { it.name == null && it.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED }
                ?.reqGlEsVersion,
        )
    }
}
