package sk.styk.martin.apkanalyzer.core.apps.packaging

import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.common.model.megabytes

class NativeLibraryItemTest {

    @Test
    fun `Given no device to judge against When grouping libraries Then nothing is marked unsupported`() {
        val files = listOf(aFile(abi = "x86"))

        val items = files.groupedByLibrary(deviceAbis = emptySet())

        items.single().isDeviceCompatible shouldBe true
    }

    @Test
    fun `Given a device without the shipped architecture When grouping libraries Then the library is marked unsupported`() {
        val files = listOf(aFile(abi = "x86"))

        val items = files.groupedByLibrary(deviceAbis = setOf("arm64-v8a"))

        items.single().isDeviceCompatible shouldBe false
    }
}

private fun aFile(abi: String) = NativeLibraryFile(name = "libfoo.so", abi = abi, size = 1.megabytes, containingApkFileName = "base.apk")
