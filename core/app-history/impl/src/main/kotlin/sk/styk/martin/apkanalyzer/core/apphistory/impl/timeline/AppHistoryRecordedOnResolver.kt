package sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline

import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryRecordedOn
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryDevice
import sk.styk.martin.apkanalyzer.core.common.device.DeviceInfoProvider
import java.util.Locale
import javax.inject.Inject

internal class AppHistoryRecordedOnResolver @Inject constructor(private val deviceInfoProvider: DeviceInfoProvider) {

    fun resolve(deviceId: String, devices: Map<String, AppHistoryDevice>): AppHistoryRecordedOn = when (deviceId) {
        deviceInfoProvider.deviceId -> AppHistoryRecordedOn.ThisDevice
        else -> AppHistoryRecordedOn.OtherDevice(devices.getValue(deviceId).displayName)
    }
}

private val AppHistoryDevice.displayName: String
    get() = if (model.startsWith(manufacturer, ignoreCase = true)) {
        model
    } else {
        "${manufacturer.replaceFirstChar { it.titlecase(Locale.ROOT) }} $model".trim()
    }
