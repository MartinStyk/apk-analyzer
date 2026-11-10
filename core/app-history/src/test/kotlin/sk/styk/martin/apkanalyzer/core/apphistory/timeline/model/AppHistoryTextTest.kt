package sk.styk.martin.apkanalyzer.core.apphistory.timeline.model

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryFile
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import sk.styk.martin.apkanalyzer.core.common.model.kilobytes
import sk.styk.martin.apkanalyzer.core.common.model.megabytes

@RunWith(RobolectricTestRunner::class)
class AppHistoryTextTest {

    private val resources = RuntimeEnvironment.getApplication().resources

    @Test
    fun `Given a library file that grew When labelling it Then it names the ABI and no total`() {
        val before = NativeLibrary("libfoo.so", listOf(aLibraryFile(10.megabytes)))
        val after = NativeLibrary("libfoo.so", listOf(aLibraryFile(11.megabytes)))

        val label = after.fieldChangesFrom(before).single().label(resources)

        label shouldBe "arm64-v8a: 10.0 MB → 11.0 MB (+1.0 MB)"
    }

    @Test
    fun `Given sizes that round to the same value When labelling them Then the precise delta tells them apart`() {
        val change = ValueChange(5_324.kilobytes, 5_340.kilobytes)

        val label = change.sizeLabel(resources)

        label shouldBe "5.2 MB → 5.2 MB (+16 KB)"
    }

    @Test
    fun `Given a delta under one kilobyte When labelling it Then it reads in bytes rather than zero kilobytes`() {
        val change = ValueChange(5_324.kilobytes, 5_324.kilobytes + 300.bytes)

        val label = change.sizeLabel(resources)

        label shouldBe "5.2 MB → 5.2 MB (+300 B)"
    }

    @Test
    fun `Given a library that shrank When labelling it Then the delta is negative`() {
        val change = ValueChange(2_048.kilobytes, 1_024.kilobytes)

        val label = change.sizeLabel(resources)

        label shouldBe "2.0 MB → 1.0 MB (−1.0 MB)"
    }

    @Test
    fun `Given two installers in the same bucket When labelling the change Then the package names tell them apart`() {
        val change = ValueChange(anInstallSource(AppSource.Unknown, "com.apkpure.aegon"), anInstallSource(AppSource.Unknown, "com.other.store"))

        val label = change.installSourceLabel(resources)

        label shouldBe "com.apkpure.aegon → com.other.store"
    }

    @Test
    fun `Given a missing installer in the same bucket When labelling the change Then it reads as the source and never null`() {
        val change = ValueChange(anInstallSource(AppSource.Unknown, null), anInstallSource(AppSource.Unknown, "com.other.store"))

        val label = change.installSourceLabel(resources)

        label shouldBe "Unknown → com.other.store"
    }

    @Test
    fun `Given different sources When labelling the change Then labels are shown rather than packages`() {
        val change = ValueChange(anInstallSource(AppSource.GooglePlay, "com.android.vending"), anInstallSource(AppSource.Sideloaded, "com.android.chrome"))

        val label = change.installSourceLabel(resources)

        label shouldBe "Google Play → Installed from a file"
    }

    @Test
    fun `Given an installer change in the same bucket When summarizing Then the part names the package`() {
        val part = AppHistorySummaryPart.InstallSourceChanged(
            ValueChange(anInstallSource(AppSource.Unknown, "com.apkpure.aegon"), anInstallSource(AppSource.Unknown, "com.other.store")),
        )

        val label = part.label(resources)

        label shouldBe "Install source changed to com.other.store"
    }

    @Test
    fun `Given five change lines When collapsing them Then two lines remain and the rest are counted`() {
        val lines = listOf("a", "b", "c", "d", "e")

        val collapsed = lines.collapsedToTwoLines(resources)

        collapsed shouldBe listOf("a", "b · and 3 more")
    }
}

private fun aLibraryFile(size: AppSize) = NativeLibraryFile(name = "libfoo.so", abi = "arm64-v8a", size = size, containingApkFileName = "base.apk")

private fun anInstallSource(source: AppSource, installer: String?) = AppHistoryInstallSource(source, installer?.let(::PackageName))
