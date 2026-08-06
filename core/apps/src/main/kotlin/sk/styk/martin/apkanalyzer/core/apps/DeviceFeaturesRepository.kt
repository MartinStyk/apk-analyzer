package sk.styk.martin.apkanalyzer.core.apps

import sk.styk.martin.apkanalyzer.core.apps.model.DeviceFeatures

interface DeviceFeaturesRepository {
    suspend fun deviceFeatures(): DeviceFeatures
}
