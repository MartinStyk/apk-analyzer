package sk.styk.martin.apkanalyzer.core.apps.devicefeatures

interface DeviceFeaturesRepository {
    suspend fun deviceFeatures(): DeviceFeatures
}
