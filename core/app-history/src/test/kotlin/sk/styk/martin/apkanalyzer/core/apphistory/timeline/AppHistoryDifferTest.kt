package sk.styk.martin.apkanalyzer.core.apphistory.timeline

import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryCaptureKey
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.storage.CapturedSection
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.SetChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.model.Detail.WhatsApp
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryFile
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import java.time.Instant

class AppHistoryDifferTest {

    private val differ = AppHistoryDiffer()

    @Test
    fun `Given launcher status known on only one side When diffing Then the activity is not reported as changed`() {
        val before = aRecord(activities = listOf(anActivity(isLauncher = null)))
        val after = aRecord(activities = listOf(anActivity(isLauncher = true)))

        val diff = differ(before, after)

        diff.activities shouldBe SetChange.Unchanged
    }

    @Test
    fun `Given an activity that became exported When diffing Then it is modified`() {
        val old = anActivity(isExported = false)
        val new = anActivity(isExported = true)

        val diff = differ(aRecord(activities = listOf(old)), aRecord(activities = listOf(new)))

        diff.activities shouldBe SetChange.Changed(
            added = emptyList(),
            removed = emptyList(),
            modified = listOf(ValueChange(old, new)),
        )
    }

    @Test
    fun `Given a library with one changed and one unchanged ABI When diffing Then only the changed file is modified`() {
        val unchanged = aLibraryFile("x86_64", 5.megabytes)
        val old = aLibraryFile("arm64-v8a", 10.megabytes)
        val new = aLibraryFile("arm64-v8a", 11.megabytes)
        val before = aRecord(activities = emptyList(), libraries = listOf(old, unchanged))
        val after = aRecord(activities = emptyList(), libraries = listOf(new, unchanged))

        val diff = differ(before, after)

        diff.nativeLibraries shouldBe SetChange.Changed(added = emptyList(), removed = emptyList(), modified = listOf(ValueChange(old, new)))
    }
}

private fun anActivity(isExported: Boolean = false, isLauncher: Boolean? = false) = Activity(
    name = "com.whatsapp.Main",
    packageName = WhatsApp.info.packageName,
    isExported = isExported,
    isLauncher = isLauncher,
)

private fun aLibraryFile(abi: String, size: AppSize) = NativeLibraryFile(name = "libfoo.so", abi = abi, size = size, containingApkFileName = "base.apk")

private fun aRecord(activities: List<Activity>, libraries: List<NativeLibraryFile> = emptyList()) = AppHistoryRecord(
    packageName = WhatsApp.info.packageName,
    capturedAt = Instant.ofEpochMilli(1_000),
    captureKey = AppHistoryCaptureKey(
        deviceId = "device",
        firstInstallTime = Instant.ofEpochMilli(1_000),
        lastUpdateTime = Instant.ofEpochMilli(1_000),
        versionCode = 1,
    ),
    detail = WhatsApp.copy(activities = activities),
    nativeLibraries = CapturedSection.Captured(NativeLibraries(libraries)),
    signingScheme = CapturedSection.Captured(emptyList()),
)
