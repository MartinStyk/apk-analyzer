package sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import dagger.Lazy
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryDeviceTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryPersistenceRepositoryFake
import sk.styk.martin.apkanalyzer.core.common.coroutines.TestDispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.device.DeviceInfoProviderFake
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTrackerFake
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlag
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepositoryFake
import java.io.IOException

class AppHistoryPreloaderTest {

    @get:Rule
    val dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher())

    private val appScope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private val persistence = AppHistoryPersistenceRepositoryFake()
    private val repository = AppHistoryRepositoryImpl(
        persistence,
        AppHistoryTimelineBuilder(AppHistoryDiffer(), AppHistoryRecordedOnResolver(DeviceInfoProviderFake())),
        PerformanceTrackerFake(),
        dispatcherProvider,
        appScope,
    )
    private val featureFlags = FeatureFlagRepositoryFake(FeatureFlag.WhatChangedTab)
    private val deviceInfo = DeviceInfoProviderFake()
    private var repositoryRequests = 0
    private val preloader = AppHistoryPreloader(
        historyRepository = Lazy {
            repositoryRequests++
            repository
        },
        featureFlagRepository = featureFlags,
        deviceInfoProvider = deviceInfo,
        appScope = appScope,
        dispatcherProvider = dispatcherProvider,
    )
    private val owner = object : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
    }

    @After
    fun tearDown() {
        appScope.cancel()
    }

    @Test
    fun `Given startup When twenty seconds pass Then history is preloaded once without ongoing observation`() = runTest {
        preloader.onCreate(owner)
        preloader.onCreate(owner)
        runCurrent()
        advanceTimeBy(19_999)
        runCurrent()
        repositoryRequests shouldBe 0
        persistence.headerObservationCount shouldBe 0

        advanceTimeBy(1)
        runCurrent()

        repositoryRequests shouldBe 1
        persistence.headerObservationCount shouldBe 1
        persistence.headerObservers shouldBe 0
        repository.deviceTimeline().first() shouldBe AppHistoryDeviceTimeline.Available(emptyList())
    }

    @Test
    fun `Given a low RAM device When the preload delay expires Then history is not requested`() = runTest {
        deviceInfo.isLowRamDevice = true
        preloader.onCreate(owner)
        runCurrent()

        advanceTimeBy(20_000)
        runCurrent()

        repositoryRequests shouldBe 0
        persistence.headerObservationCount shouldBe 0
        repository.deviceTimeline().first() shouldBe AppHistoryDeviceTimeline.Available(emptyList())
    }

    @Test
    fun `Given What changed is disabled at startup When time passes Then history is not requested`() = runTest {
        featureFlags[FeatureFlag.WhatChangedTab] = false
        preloader.onCreate(owner)
        runCurrent()

        advanceTimeBy(20_000)
        runCurrent()

        repositoryRequests shouldBe 0
        persistence.headerObservationCount shouldBe 0
    }

    @Test
    fun `Given preload fails When history is opened later Then loading can recover`() = runTest {
        persistence.headersFailure = IOException("disk")
        preloader.onCreate(owner)
        runCurrent()
        advanceTimeBy(20_000)
        runCurrent()
        persistence.headerObservers shouldBe 0
        persistence.headersFailure = null

        val timeline = repository.deviceTimeline().first { it is AppHistoryDeviceTimeline.Available }

        timeline shouldBe AppHistoryDeviceTimeline.Available(emptyList())
        persistence.headerObservationCount shouldBe 2
    }
}
