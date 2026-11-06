package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.appfunctions.model.AppSummary
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepositoryFake
import sk.styk.martin.apkanalyzer.core.apps.model.anApp
import sk.styk.martin.apkanalyzer.core.apps.storagestats.StorageStatsRepositoryFake
import sk.styk.martin.apkanalyzer.core.apps.usagestats.UsageStatsRepositoryFake
import sk.styk.martin.apkanalyzer.core.common.coroutines.TestDispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import java.time.Instant
import java.time.temporal.ChronoUnit

class FindAppsUseCaseTest {

    @get:Rule
    val dispatcherProvider = TestDispatcherProvider()

    private val installedAppsRepository = InstalledAppsRepositoryFake()
    private val useCase = FindAppsUseCase(
        installedAppsRepository,
        StorageStatsRepositoryFake(),
        UsageStatsRepositoryFake(),
        dispatcherProvider,
    )

    @Test
    fun `Given a query in a different case When finding apps Then app name matches`() = runTest {
        installedAppsRepository += anApp(name = "Gmail", packageName = "com.example.first")
        installedAppsRepository += anApp(name = "Notes", packageName = "com.example.second")

        val apps = useCase(searchFilters(query = "MAIL"), sortBy = null)

        apps shouldBe listOf(anAppSummary(name = "Gmail", packageName = "com.example.first"))
    }

    @Test
    fun `Given a query in a different case When finding apps Then package name matches`() = runTest {
        installedAppsRepository += anApp(name = "Messenger", packageName = "com.whatsapp")
        installedAppsRepository += anApp(name = "Notes")

        val apps = useCase(searchFilters(query = "COM.WHATSAPP"), sortBy = null)

        apps shouldBe listOf(anAppSummary(name = "Messenger", packageName = "com.whatsapp"))
    }

    @Test
    fun `Given a permission filter When finding apps Then only apps requesting it are returned`() = runTest {
        installedAppsRepository += anApp(name = "Requesting", requestedPermissions = listOf("android.permission.READ_SMS"))
        installedAppsRepository += anApp(name = "Other", requestedPermissions = listOf("android.permission.CAMERA"))

        val apps = useCase(searchFilters(permission = "android.permission.READ_SMS"), sortBy = null)

        apps shouldBe listOf(anAppSummary(name = "Requesting"))
    }

    @Test
    fun `Given an install source filter When finding apps Then only apps from that source are returned`() = runTest {
        installedAppsRepository += anApp(name = "Matching", source = AppSource.FDroid)
        installedAppsRepository += anApp(name = "Other", source = AppSource.GooglePlay)

        val apps = useCase(searchFilters(installSource = AppSource.FDroid), sortBy = null)

        apps shouldBe listOf(anAppSummary(name = "Matching", installSourceLabel = "F-Droid"))
    }

    @Test
    fun `Given a target SDK filter When finding apps Then only apps targeting it are returned`() = runTest {
        installedAppsRepository += anApp(name = "Matching", targetSdk = 21)
        installedAppsRepository += anApp(name = "Other", targetSdk = 36)

        val apps = useCase(searchFilters(targetSdk = 21), sortBy = null)

        apps shouldBe listOf(anAppSummary(name = "Matching"))
    }

    @Test
    fun `Given multiple filters When finding apps Then only apps matching all of them are returned`() = runTest {
        installedAppsRepository += anApp(name = "Both", targetSdk = 21, source = AppSource.FDroid)
        installedAppsRepository += anApp(name = "OnlySdk", targetSdk = 21, source = AppSource.GooglePlay)
        installedAppsRepository += anApp(name = "OnlySource", targetSdk = 36, source = AppSource.FDroid)

        val apps = useCase(searchFilters(targetSdk = 21, installSource = AppSource.FDroid), sortBy = null)

        apps shouldBe listOf(anAppSummary(name = "Both", installSourceLabel = "F-Droid"))
    }

    @Test
    fun `Given total size and APK size When filtering by minimum size Then total size decides`() = runTest {
        installedAppsRepository += anApp(name = "LargeTotal", totalSize = 200.megabytes, apkSize = 1.megabytes)
        installedAppsRepository += anApp(name = "LargeApk", totalSize = 1.megabytes, apkSize = 200.megabytes)

        val apps = useCase(searchFilters(minTotalSizeMb = 100), sortBy = null)

        apps shouldBe listOf(anAppSummary(name = "LargeTotal", sizeMb = 200.0))
    }

    @Test
    fun `Given no total size When filtering by minimum size Then APK size decides`() = runTest {
        installedAppsRepository += anApp(name = "Large", totalSize = null, apkSize = 200.megabytes)
        installedAppsRepository += anApp(name = "Small", totalSize = null, apkSize = 1.megabytes)

        val apps = useCase(searchFilters(minTotalSizeMb = 100), sortBy = null)

        apps shouldBe listOf(anAppSummary(name = "Large", sizeMb = 200.0))
    }

    @Test
    fun `Given recently used, long unused, and never used apps When filtering by unused days Then recently used app is excluded`() = runTest {
        val now = Instant.now()
        val longAgo = now.minus(60, ChronoUnit.DAYS)
        installedAppsRepository += anApp(name = "Recent", lastUsedTime = now.minus(1, ChronoUnit.DAYS))
        installedAppsRepository += anApp(name = "LongUnused", lastUsedTime = longAgo)
        installedAppsRepository += anApp(name = "NeverUsed", lastUsedTime = null)

        val apps = useCase(searchFilters(unusedForDays = 30), sortBy = null)

        apps shouldBe listOf(
            anAppSummary(name = "LongUnused", lastUsedTime = longAgo),
            anAppSummary(name = "NeverUsed", lastUsedTime = null),
        )
    }

    @Test
    fun `Given no sort order When finding apps Then apps are sorted by name`() = runTest {
        installedAppsRepository += anApp(name = "Cherry")
        installedAppsRepository += anApp(name = "Apple")
        installedAppsRepository += anApp(name = "Banana")

        val apps = useCase(searchFilters(targetSdk = 36), sortBy = null)

        apps shouldBe listOf(
            anAppSummary(name = "Apple"),
            anAppSummary(name = "Banana"),
            anAppSummary(name = "Cherry"),
        )
    }

    @Test
    fun `Given names with mixed case and accents When sorting by name Then case and accents do not split them`() = runTest {
        installedAppsRepository += anApp(name = "Zebra")
        installedAppsRepository += anApp(name = "Éclair")
        installedAppsRepository += anApp(name = "apple")

        val apps = useCase(searchFilters(), SortBy.Name)

        apps.map { it.applicationName } shouldBe listOf("apple", "Éclair", "Zebra")
    }

    @Test
    fun `Given a descending size sort When finding apps Then APK size stands in for a missing total size`() = runTest {
        installedAppsRepository += anApp(name = "Small", totalSize = 100.megabytes, apkSize = 900.megabytes)
        installedAppsRepository += anApp(name = "Large", totalSize = null, apkSize = 300.megabytes)
        installedAppsRepository += anApp(name = "Medium", totalSize = 200.megabytes, apkSize = 1.megabytes)

        val apps = useCase(searchFilters(), SortBy.SizeDescending)

        apps shouldBe listOf(
            anAppSummary(name = "Large", sizeMb = 300.0),
            anAppSummary(name = "Medium", sizeMb = 200.0),
            anAppSummary(name = "Small", sizeMb = 100.0),
        )
    }

    @Test
    fun `Given an ascending size sort When finding apps Then smallest app is first`() = runTest {
        installedAppsRepository += anApp(name = "Large", totalSize = 300.megabytes)
        installedAppsRepository += anApp(name = "Small", totalSize = 100.megabytes)
        installedAppsRepository += anApp(name = "Medium", totalSize = 200.megabytes)

        val apps = useCase(searchFilters(), SortBy.SizeAscending)

        apps shouldBe listOf(
            anAppSummary(name = "Small", sizeMb = 100.0),
            anAppSummary(name = "Medium", sizeMb = 200.0),
            anAppSummary(name = "Large", sizeMb = 300.0),
        )
    }

    @Test
    fun `Given an ascending last used sort When finding apps Then never used app is first`() = runTest {
        val january = Instant.parse("2026-01-01T00:00:00Z")
        val february = Instant.parse("2026-02-01T00:00:00Z")
        installedAppsRepository += anApp(name = "Newer", lastUsedTime = february)
        installedAppsRepository += anApp(name = "Never", lastUsedTime = null)
        installedAppsRepository += anApp(name = "Older", lastUsedTime = january)

        val apps = useCase(searchFilters(), SortBy.LastUsedAscending)

        apps shouldBe listOf(
            anAppSummary(name = "Never", lastUsedTime = null),
            anAppSummary(name = "Older", lastUsedTime = january),
            anAppSummary(name = "Newer", lastUsedTime = february),
        )
    }

    @Test
    fun `Given a descending last used sort When finding apps Then never used app is last`() = runTest {
        val january = Instant.parse("2026-01-01T00:00:00Z")
        val february = Instant.parse("2026-02-01T00:00:00Z")
        installedAppsRepository += anApp(name = "Never", lastUsedTime = null)
        installedAppsRepository += anApp(name = "Older", lastUsedTime = january)
        installedAppsRepository += anApp(name = "Newer", lastUsedTime = february)

        val apps = useCase(searchFilters(), SortBy.LastUsedDescending)

        apps shouldBe listOf(
            anAppSummary(name = "Newer", lastUsedTime = february),
            anAppSummary(name = "Older", lastUsedTime = january),
            anAppSummary(name = "Never", lastUsedTime = null),
        )
    }

    @Test
    fun `Given more apps than the limit When finding apps Then first 25 after sorting are returned`() = runTest {
        (39 downTo 10).forEach { number ->
            installedAppsRepository += anApp(name = "App$number")
        }

        val apps = useCase(searchFilters(), SortBy.Name)

        apps shouldBe (10..34).map { number -> anAppSummary(name = "App$number") }
    }

    @Test
    fun `Given a matching app When finding apps Then summary contains its details`() = runTest {
        installedAppsRepository += anApp(
            name = "Gmail",
            packageName = "com.google.android.gm",
            versionName = "1.2.3",
            source = AppSource.FDroid,
            totalSize = 2.megabytes,
            apkSize = 9.megabytes,
            lastUsedTime = Instant.parse("2026-01-01T00:00:00Z"),
        )

        val apps = useCase(searchFilters(query = "gmail"), sortBy = null)

        apps shouldBe listOf(
            AppSummary(
                packageName = "com.google.android.gm",
                applicationName = "Gmail",
                versionName = "1.2.3",
                installSourceLabel = "F-Droid",
                sizeMb = 2.0,
                lastUsedTime = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
    }
}

private val defaultLastUsedTime = Instant.parse("2026-03-01T00:00:00Z")

private fun packageNameFor(name: String) = "com.example.${name.lowercase()}"

private fun anAppSummary(
    name: String,
    packageName: String = packageNameFor(name),
    versionName: String = "1.0",
    installSourceLabel: String = "Google Play",
    sizeMb: Double = 10.0,
    lastUsedTime: Instant? = defaultLastUsedTime,
) = AppSummary(
    packageName = packageName,
    applicationName = name,
    versionName = versionName,
    installSourceLabel = installSourceLabel,
    sizeMb = sizeMb,
    lastUsedTime = lastUsedTime,
)

private fun searchFilters(
    query: String? = null,
    permission: String? = null,
    installSource: AppSource? = null,
    targetSdk: Int? = null,
    minTotalSizeMb: Int? = null,
    unusedForDays: Int? = null,
) = SearchFilters(query, permission, installSource, targetSdk, minTotalSizeMb, unusedForDays)
