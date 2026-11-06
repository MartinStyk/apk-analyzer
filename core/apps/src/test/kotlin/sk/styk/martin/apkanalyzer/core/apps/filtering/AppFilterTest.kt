package sk.styk.martin.apkanalyzer.core.apps.filtering

import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apps.model.anApp
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import java.time.Instant
import java.time.temporal.ChronoUnit

class AppFilterTest {

    @Test
    fun `Given an inactive filter When filtering Then all apps are returned`() {
        val apps = listOf(anApp(name = "First"), anApp(name = "Second"))

        val result = apps.filterBy(AppFilterState())

        result shouldBe apps
    }

    @Test
    fun `Given selected sources When filtering Then only apps from those sources are returned`() {
        val play = anApp(name = "Play", source = AppSource.GooglePlay)
        val fDroid = anApp(name = "FDroid", source = AppSource.FDroid)
        val sideloaded = anApp(name = "Sideloaded", source = AppSource.Sideloaded)

        val result = listOf(play, fDroid, sideloaded).filterBy(AppFilterState(selectedSources = setOf(AppSource.GooglePlay, AppSource.FDroid)))

        result shouldBe listOf(play, fDroid)
    }

    @Test
    fun `Given multiple filters When filtering Then only apps matching all of them are returned`() {
        val both = anApp(name = "Both", source = AppSource.FDroid, targetSdk = 21)
        val onlySource = anApp(name = "OnlySource", source = AppSource.FDroid, targetSdk = 36)
        val onlySdk = anApp(name = "OnlySdk", source = AppSource.GooglePlay, targetSdk = 21)
        val filter = AppFilterState(selectedSources = setOf(AppSource.FDroid), selectedSdkVersions = setOf(21))

        val result = listOf(both, onlySource, onlySdk).filterBy(filter)

        result shouldBe listOf(both)
    }

    @Test
    fun `Given an APK size range When filtering Then both ends of the range are included`() {
        val atMin = anApp(name = "AtMin", apkSize = 10.megabytes)
        val atMax = anApp(name = "AtMax", apkSize = 20.megabytes)
        val below = anApp(name = "Below", apkSize = 9.megabytes)
        val above = anApp(name = "Above", apkSize = 21.megabytes)
        val filter = AppFilterState(apkSizeRange = AppSizeRange(min = 10.megabytes, max = 20.megabytes))

        val result = listOf(atMin, atMax, below, above).filterBy(filter)

        result shouldBe listOf(atMin, atMax)
    }

    @Test
    fun `Given a total size range When filtering Then app with unknown total size is kept`() {
        val inRange = anApp(name = "InRange", totalSize = 15.megabytes)
        val unknown = anApp(name = "Unknown", totalSize = null)
        val outOfRange = anApp(name = "OutOfRange", totalSize = 50.megabytes)
        val filter = AppFilterState(totalSizeRange = AppSizeRange(min = 10.megabytes, max = 20.megabytes))

        val result = listOf(inRange, unknown, outOfRange).filterBy(filter)

        result shouldBe listOf(inRange, unknown)
    }

    @Test
    fun `Given an install date range When filtering Then install time decides and both ends are included`() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val end = Instant.parse("2026-02-01T00:00:00Z")
        val atStart = anApp(name = "AtStart", installTime = start, lastUpdateTime = Instant.parse("2020-01-01T00:00:00Z"))
        val atEnd = anApp(name = "AtEnd", installTime = end)
        val outside = anApp(name = "Outside", installTime = Instant.parse("2020-01-01T00:00:00Z"), lastUpdateTime = start)

        val result = listOf(atStart, atEnd, outside).filterBy(AppFilterState(installTimeRange = DateRange(start, end)))

        result shouldBe listOf(atStart, atEnd)
    }

    @Test
    fun `Given an update date range When filtering Then update time decides`() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val end = Instant.parse("2026-02-01T00:00:00Z")
        val updated = anApp(name = "Updated", installTime = Instant.parse("2020-01-01T00:00:00Z"), lastUpdateTime = start)
        val installed = anApp(name = "Installed", installTime = start, lastUpdateTime = Instant.parse("2020-01-01T00:00:00Z"))

        val result = listOf(updated, installed).filterBy(AppFilterState(updateTimeRange = DateRange(start, end)))

        result shouldBe listOf(updated)
    }

    @Test
    fun `Given an unused period When filtering Then recently used app is excluded and never used app is kept`() {
        val now = Instant.now()
        val recent = anApp(name = "Recent", lastUsedTime = now.minus(1, ChronoUnit.DAYS))
        val longUnused = anApp(name = "LongUnused", lastUsedTime = now.minus(60, ChronoUnit.DAYS))
        val neverUsed = anApp(name = "NeverUsed", lastUsedTime = null)

        val result = listOf(recent, longUnused, neverUsed).filterBy(AppFilterState(unusedPeriod = UnusedAppsPeriod.ONE_MONTH))

        result shouldBe listOf(longUnused, neverUsed)
    }

    @Test
    fun `Given a recently used period When filtering Then only recently used app is kept`() {
        val now = Instant.now()
        val recent = anApp(name = "Recent", lastUsedTime = now.minus(1, ChronoUnit.DAYS))
        val longUnused = anApp(name = "LongUnused", lastUsedTime = now.minus(60, ChronoUnit.DAYS))
        val neverUsed = anApp(name = "NeverUsed", lastUsedTime = null)

        val result = listOf(recent, longUnused, neverUsed).filterBy(AppFilterState(recentlyUsedDays = 7))

        result shouldBe listOf(recent)
    }

    @Test
    fun `Given selected permissions matching any When filtering Then app with one of them is kept`() {
        val one = anApp(name = "One", requestedPermissions = listOf("camera"))
        val none = anApp(name = "None", requestedPermissions = listOf("internet"))
        val filter = AppFilterState(selectedPermissions = setOf("camera", "microphone"), permissionMatchAll = false)

        val result = listOf(one, none).filterBy(filter)

        result shouldBe listOf(one)
    }

    @Test
    fun `Given selected permissions matching all When filtering Then app must have every one of them`() {
        val both = anApp(name = "Both", requestedPermissions = listOf("camera", "microphone", "internet"))
        val one = anApp(name = "One", requestedPermissions = listOf("camera"))
        val filter = AppFilterState(selectedPermissions = setOf("camera", "microphone"), permissionMatchAll = true)

        val result = listOf(both, one).filterBy(filter)

        result shouldBe listOf(both)
    }
}
