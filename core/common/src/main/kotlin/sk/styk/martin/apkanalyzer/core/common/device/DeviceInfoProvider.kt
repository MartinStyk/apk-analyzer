package sk.styk.martin.apkanalyzer.core.common.device

interface DeviceInfoProvider {
    val deviceId: String
    val manufacturer: String
    val model: String
}
