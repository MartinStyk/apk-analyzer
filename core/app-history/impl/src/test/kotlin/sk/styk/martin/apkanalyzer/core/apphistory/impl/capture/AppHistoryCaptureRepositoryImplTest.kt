package sk.styk.martin.apkanalyzer.core.apphistory.impl.capture

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryCaptureKey
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryPersistenceRepositoryFake
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.CapturedSection
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.StoredAppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline.AppHistoryDiffer
import sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline.AppHistoryRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline.AppHistoryTimelineBuilder
import sk.styk.martin.apkanalyzer.core.apps.api.AppDetailRepositoryFake
import sk.styk.martin.apkanalyzer.core.apps.api.InstalledAppsRepositoryFake
import sk.styk.martin.apkanalyzer.core.apps.api.model.Detail.WhatsApp
import sk.styk.martin.apkanalyzer.core.apps.api.model.anApp
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibrariesRepositoryFake
import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeRepositoryFake
import sk.styk.martin.apkanalyzer.core.common.coroutines.TestDispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.device.DeviceInfoProviderFake
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTrackerFake
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepositoryFake
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlag
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepositoryFake
import java.io.IOException
import java.time.Instant

class AppHistoryCaptureRepositoryImplTest {

    @get:Rule
    val dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher())

    private val installedApp = anApp(name = "WhatsApp", packageName = WhatsApp.info.packageName.value).copy(version = 2)
    private val detail = WhatsApp.copy(
        info = WhatsApp.info.copy(
            versionCode = 2,
            versionName = "2.0",
            firstInstallTime = installedApp.installTime,
            lastUpdateTime = installedApp.lastUpdateTime,
        ),
    )
    private val installedApps = InstalledAppsRepositoryFake(installedApp)
    private val details = AppDetailRepositoryFake(detail)
    private val persistence = AppHistoryPersistenceRepositoryFake()
    private val featureFlags = FeatureFlagRepositoryFake(FeatureFlag.AppHistoryRecording)
    private val preferences = PersistenceRepositoryFake(Key.AppHistoryRecordingEnabled to true)
    private val appScope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private val repository = AppHistoryCaptureRepositoryImpl(
        installedAppsRepository = installedApps,
        appDetailRepository = details,
        nativeLibrariesRepository = NativeLibrariesRepositoryFake(),
        signingSchemeRepository = SigningSchemeRepositoryFake(),
        persistenceRepository = persistence,
        deviceInfoProvider = DeviceInfoProviderFake(),
        dispatcherProvider = dispatcherProvider,
        performanceTracker = PerformanceTrackerFake(),
        recordingRepository = AppHistoryRecordingRepositoryImpl(featureFlags, preferences),
        appScope = appScope,
    )
    private val history =
        AppHistoryRepositoryImpl(persistence, AppHistoryTimelineBuilder(AppHistoryDiffer()), PerformanceTrackerFake(), dispatcherProvider, appScope)

    @After
    fun tearDown() {
        appScope.cancel()
    }

    @Test
    fun `Given an older snapshot When capture is requested Then the open timeline receives the installed version`() = runTest {
        persistence += anOlderRecord()
        val versions = mutableListOf<List<Long>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            history.timeline(installedApp.packageName).collect { timeline ->
                versions += (timeline as AppHistoryTimeline.Available).entries.map { it.snapshot.version.versionCode }
            }
        }
        runCurrent()

        repository.requestCapture(installedApp.packageName)
        runCurrent()

        versions shouldBe listOf(listOf(1L), listOf(2L, 1L))
    }

    @Test
    fun `Given overlapping capture requests and a sweep When capture finishes Then only one snapshot is saved`() = runTest {
        val barrier = CompletableDeferred<Unit>()
        persistence.saveBarrier = barrier

        repository.requestCapture(installedApp.packageName)
        repository.requestCapture(installedApp.packageName)
        launch { repository.reconcileAll() }
        runCurrent()
        barrier.complete(Unit)
        advanceUntilIdle()
        repository.requestCapture(installedApp.packageName)
        advanceUntilIdle()

        val records = persistence.observe(installedApp.packageName).first().getOrThrow()
        records.size shouldBe 1
        records.single().record.captureKey.versionCode shouldBe 2L
    }

    @Test
    fun `Given recording is disabled When capture is requested Then no snapshot is saved`() = runTest {
        featureFlags[FeatureFlag.AppHistoryRecording] = false

        repository.requestCapture(installedApp.packageName)
        advanceUntilIdle()

        persistence.observe(installedApp.packageName).first().getOrThrow() shouldBe emptyList()
    }

    @Test
    fun `Given the user turned recording off When capture is requested Then no snapshot is saved`() = runTest {
        preferences[Key.AppHistoryRecordingEnabled] = false

        repository.requestCapture(installedApp.packageName)
        advanceUntilIdle()

        persistence.observe(installedApp.packageName).first().getOrThrow() shouldBe emptyList()
    }

    @Test
    fun `Given a failed save When capture is requested again Then old history survives and the new version is saved`() = runTest {
        val older = anOlderRecord()
        persistence += older
        persistence.saveFailure = IOException("disk")

        repository.requestCapture(installedApp.packageName)
        advanceUntilIdle()
        val afterFailure = persistence.observe(installedApp.packageName).first().getOrThrow()
        persistence.saveFailure = null
        repository.requestCapture(installedApp.packageName)
        advanceUntilIdle()

        afterFailure shouldBe listOf(older)
        persistence.observe(installedApp.packageName).first().getOrThrow().map { it.record.captureKey.versionCode } shouldBe listOf(1L, 2L)
    }

    @Test
    fun `Given capture is pending When the caller is cancelled Then capture still finishes`() = runTest {
        val barrier = CompletableDeferred<Unit>()
        persistence.saveBarrier = barrier

        val caller = launch {
            repository.requestCapture(installedApp.packageName)
            awaitCancellation()
        }
        runCurrent()
        caller.cancel()
        barrier.complete(Unit)
        advanceUntilIdle()

        persistence.observe(installedApp.packageName).first().getOrThrow().single().record.detail shouldBe detail
    }
}

private fun anOlderRecord() = StoredAppHistoryRecord(
    id = 1,
    record = AppHistoryRecord(
        packageName = WhatsApp.info.packageName,
        capturedAt = Instant.parse("2026-01-02T00:00:00Z"),
        captureKey = AppHistoryCaptureKey(
            deviceId = "device",
            firstInstallTime = Instant.parse("2026-01-01T00:00:00Z"),
            lastUpdateTime = Instant.parse("2026-01-01T00:00:00Z"),
            versionCode = 1,
        ),
        detail = WhatsApp.copy(info = WhatsApp.info.copy(versionCode = 1, versionName = "1.0")),
        nativeLibraries = CapturedSection.Captured(NativeLibraries.Empty),
        signingScheme = CapturedSection.Captured(emptyList()),
    ),
)
