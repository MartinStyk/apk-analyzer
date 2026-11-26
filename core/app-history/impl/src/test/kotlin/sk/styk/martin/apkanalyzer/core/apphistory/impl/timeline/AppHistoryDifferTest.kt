package sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline

import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryFieldChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryItemField
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.NativeLibrary
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.RuleDirection
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.SetChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.fieldChangesFrom
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryCaptureKey
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.CapturedSection
import sk.styk.martin.apkanalyzer.core.apps.api.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.api.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.api.model.Detail.WhatsApp
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibraryFile
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
    fun `Given a library with one changed and one unchanged ABI When diffing Then the library is modified once`() {
        val unchanged = aLibraryFile("x86_64", 5.megabytes)
        val old = aLibraryFile("arm64-v8a", 10.megabytes)
        val new = aLibraryFile("arm64-v8a", 11.megabytes)
        val before = aRecord(activities = emptyList(), libraries = listOf(old, unchanged))
        val after = aRecord(activities = emptyList(), libraries = listOf(new, unchanged))

        val diff = differ(before, after)

        diff.nativeLibraries shouldBe SetChange.Changed(
            added = emptyList(),
            removed = emptyList(),
            modified = listOf(ValueChange(NativeLibrary("libfoo.so", listOf(old, unchanged)), NativeLibrary("libfoo.so", listOf(new, unchanged)))),
        )
    }

    @Test
    fun `Given an optional feature that became required When diffing Then it is modified with the necessity change`() {
        val before = aRecord(activities = emptyList(), features = listOf(Feature.Hardware(name = "android.hardware.camera", isRequired = false)))
        val after = aRecord(activities = emptyList(), features = listOf(Feature.Hardware(name = "android.hardware.camera", isRequired = true)))

        val changed = differ(before, after).requirements as SetChange.Changed

        changed.modified.single().after.fieldChangesFrom(changed.modified.single().before) shouldBe
            listOf(AppHistoryFieldChange.Flag(AppHistoryItemField.Required, before = false, after = true))
    }

    @Test
    fun `Given a library that gains copies for more ABIs When diffing Then it is modified rather than added`() {
        val arm = aLibraryFile("arm64-v8a", 10.megabytes)
        val x86 = aLibraryFile("x86_64", 12.megabytes)

        val diff = differ(aRecord(activities = emptyList(), libraries = listOf(arm)), aRecord(activities = emptyList(), libraries = listOf(arm, x86)))

        val changed = diff.nativeLibraries as SetChange.Changed
        changed.added shouldBe emptyList()
        changed.modified.single().after.fieldChangesFrom(changed.modified.single().before) shouldBe
            listOf(AppHistoryFieldChange.Abis(RuleDirection.Added, listOf("x86_64")))
    }

    @Test
    fun `Given a library whose file name carries its version When it updates Then it is one renamed library, not removed and added`() {
        val old = aLibraryFile("arm64-v8a", 10.megabytes, name = "libcronet.130.0.6723.58.so")
        val new = aLibraryFile("arm64-v8a", 10.megabytes, name = "libcronet.131.0.6778.2.so")

        val diff = differ(aRecord(activities = emptyList(), libraries = listOf(old)), aRecord(activities = emptyList(), libraries = listOf(new)))

        val changed = diff.nativeLibraries as SetChange.Changed
        changed.added shouldBe emptyList()
        changed.removed shouldBe emptyList()
        changed.modified.single().after.fieldChangesFrom(changed.modified.single().before) shouldBe listOf(
            AppHistoryFieldChange.Value(AppHistoryItemField.FileName, before = "libcronet.130.0.6723.58.so", after = "libcronet.131.0.6778.2.so"),
        )
    }

    @Test
    fun `Given two versions of one library shipped side by side When one is dropped Then they are not merged`() {
        val first = aLibraryFile("arm64-v8a", 10.megabytes, name = "libfoo.1.so")
        val second = aLibraryFile("arm64-v8a", 12.megabytes, name = "libfoo.2.so")

        val diff = differ(aRecord(activities = emptyList(), libraries = listOf(first, second)), aRecord(activities = emptyList(), libraries = listOf(second)))

        diff.nativeLibraries shouldBe SetChange.Changed(
            added = emptyList(),
            removed = listOf(NativeLibrary("libfoo.1.so", listOf(first))),
            modified = emptyList(),
        )
    }

    @Test
    fun `Given libraries whose names end in digits without a separator When one is swapped Then they are not matched`() {
        val old = aLibraryFile("arm64-v8a", 10.megabytes, name = "libsqlite3.so")
        val new = aLibraryFile("arm64-v8a", 10.megabytes, name = "libsqlite.so")

        val diff = differ(aRecord(activities = emptyList(), libraries = listOf(old)), aRecord(activities = emptyList(), libraries = listOf(new)))

        diff.nativeLibraries shouldBe SetChange.Changed(
            added = listOf(NativeLibrary("libsqlite.so", listOf(new))),
            removed = listOf(NativeLibrary("libsqlite3.so", listOf(old))),
            modified = emptyList(),
        )
    }
}

private fun anActivity(isExported: Boolean = false, isLauncher: Boolean? = false) = Activity(
    name = "com.whatsapp.Main",
    packageName = WhatsApp.info.packageName,
    isExported = isExported,
    isLauncher = isLauncher,
)

private fun aLibraryFile(
    abi: String,
    size: AppSize,
    name: String = "libfoo.so",
) = NativeLibraryFile(name = name, abi = abi, size = size, containingApkFileName = "base.apk")

private fun aRecord(
    activities: List<Activity>,
    libraries: List<NativeLibraryFile> = emptyList(),
    features: List<Feature> = emptyList(),
) = AppHistoryRecord(
    packageName = WhatsApp.info.packageName,
    capturedAt = Instant.ofEpochMilli(1_000),
    captureKey = AppHistoryCaptureKey(
        deviceId = "device",
        firstInstallTime = Instant.ofEpochMilli(1_000),
        lastUpdateTime = Instant.ofEpochMilli(1_000),
        versionCode = 1,
    ),
    detail = WhatsApp.copy(activities = activities, features = features),
    nativeLibraries = CapturedSection.Captured(NativeLibraries(libraries)),
    signingScheme = CapturedSection.Captured(emptyList()),
)
