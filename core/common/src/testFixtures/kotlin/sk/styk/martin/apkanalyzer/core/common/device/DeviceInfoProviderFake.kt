package sk.styk.martin.apkanalyzer.core.common.device

class DeviceInfoProviderFake(
    override val deviceId: String = "device",
    override val manufacturer: String = "Google",
    override val model: String = "Pixel",
) : DeviceInfoProvider
