package sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline

import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryRecordedOn
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryDevice
import sk.styk.martin.apkanalyzer.core.common.device.DeviceInfoProviderFake

class AppHistoryRecordedOnResolverTest {

    private val resolver = AppHistoryRecordedOnResolver(DeviceInfoProviderFake(deviceId = "this-phone"))

    @Test
    fun `Given the id of this phone When resolving Then it is this device even with a stored name`() {
        val devices = mapOf("this-phone" to AppHistoryDevice(manufacturer = "OnePlus", model = "KB2003"))

        val recordedOn = resolver.resolve("this-phone", devices)

        recordedOn shouldBe AppHistoryRecordedOn.ThisDevice
    }

    @Test
    fun `Given another phone When resolving Then it is named by manufacturer and model`() {
        val devices = mapOf("pixel" to AppHistoryDevice(manufacturer = "Google", model = "Pixel 7"))

        val recordedOn = resolver.resolve("pixel", devices)

        recordedOn shouldBe AppHistoryRecordedOn.OtherDevice("Google Pixel 7")
    }

    @Test
    fun `Given a lowercase manufacturer When resolving another phone Then the manufacturer is capitalized`() {
        val devices = mapOf("galaxy" to AppHistoryDevice(manufacturer = "samsung", model = "SM-S918B"))

        val recordedOn = resolver.resolve("galaxy", devices)

        recordedOn shouldBe AppHistoryRecordedOn.OtherDevice("Samsung SM-S918B")
    }

    @Test
    fun `Given a model that starts with its manufacturer When resolving another phone Then the manufacturer is not repeated`() {
        val devices = mapOf("oneplus" to AppHistoryDevice(manufacturer = "OnePlus", model = "ONEPLUS A6013"))

        val recordedOn = resolver.resolve("oneplus", devices)

        recordedOn shouldBe AppHistoryRecordedOn.OtherDevice("ONEPLUS A6013")
    }
}
