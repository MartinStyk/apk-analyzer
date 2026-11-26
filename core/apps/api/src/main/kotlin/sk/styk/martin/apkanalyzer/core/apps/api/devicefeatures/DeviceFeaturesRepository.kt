package sk.styk.martin.apkanalyzer.core.apps.api.devicefeatures

interface DeviceFeaturesRepository {
    suspend fun deviceFeatures(): DeviceFeatures
}
