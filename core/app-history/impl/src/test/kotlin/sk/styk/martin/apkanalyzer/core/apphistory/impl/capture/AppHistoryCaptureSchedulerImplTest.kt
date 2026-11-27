package sk.styk.martin.apkanalyzer.core.apphistory.impl.capture

import android.content.Context
import androidx.room.Room
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import dagger.Lazy
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import sk.styk.martin.apkanalyzer.core.apphistory.impl.restore.AppHistoryRestoreMergerImpl
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryPersistenceRepositoryFake
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryStorageCleanup
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.AppHistoryDatabase
import sk.styk.martin.apkanalyzer.core.apps.api.AppDetailRepositoryFake
import sk.styk.martin.apkanalyzer.core.apps.api.InstalledAppsRepositoryFake
import sk.styk.martin.apkanalyzer.core.apps.api.PackageChangeAction
import sk.styk.martin.apkanalyzer.core.apps.api.PackageChangeEvent
import sk.styk.martin.apkanalyzer.core.apps.api.PackageChangesObserverFake
import sk.styk.martin.apkanalyzer.core.apps.api.model.Detail.WhatsApp
import sk.styk.martin.apkanalyzer.core.apps.api.model.anApp
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibrariesRepositoryFake
import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeRepositoryFake
import sk.styk.martin.apkanalyzer.core.common.configuration.ConfigurationKey
import sk.styk.martin.apkanalyzer.core.common.configuration.ConfigurationRepositoryFake
import sk.styk.martin.apkanalyzer.core.common.coroutines.TestDispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.device.DeviceInfoProviderFake
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTrackerFake
import java.io.IOException
import java.util.concurrent.ExecutionException

@RunWith(RobolectricTestRunner::class)
class AppHistoryCaptureSchedulerImplTest {

    @get:Rule
    val dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher())

    private val context: Context = RuntimeEnvironment.getApplication()
    private val database = Room.databaseBuilder(context, AppHistoryDatabase::class.java, "app_history_v2.db").build()
    private val cleanup = AppHistoryStorageCleanup(context)
    private val appScope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private val configuration = ConfigurationRepositoryFake(ConfigurationKey.AppHistoryRecordingEnabled)
    private val packageChanges = PackageChangesObserverFake()
    private val installedApp = anApp(name = "WhatsApp", packageName = WhatsApp.info.packageName.value)
    private val persistence = AppHistoryPersistenceRepositoryFake()
    private val captureRepository = AppHistoryCaptureRepositoryImpl(
        installedAppsRepository = InstalledAppsRepositoryFake(installedApp),
        appDetailRepository = AppDetailRepositoryFake(WhatsApp),
        nativeLibrariesRepository = NativeLibrariesRepositoryFake(),
        signingSchemeRepository = SigningSchemeRepositoryFake(),
        persistenceRepository = persistence,
        deviceInfoProvider = DeviceInfoProviderFake(),
        dispatcherProvider = dispatcherProvider,
        performanceTracker = PerformanceTrackerFake(),
        configurationRepository = configuration,
        appScope = appScope,
    )
    private val workManager = mockk<WorkManager>()
    private val completedOperation = mockk<Operation> {
        every { result } returns mockk {
            every { isDone } returns true
            every { get() } returns Operation.SUCCESS
        }
    }
    private val failedOperation = mockk<Operation> {
        every { result } returns mockk {
            every { isDone } returns true
            every { get() } throws ExecutionException(IOException("Cancellation could not be persisted"))
        }
    }
    private val scheduler: AppHistoryCaptureScheduler = AppHistoryCaptureSchedulerImpl(
        captureRepository = captureRepository,
        packageChangesObserver = packageChanges,
        workManager = Lazy { workManager },
        restoreMerger = AppHistoryRestoreMergerImpl(context, database.appHistoryWriteDao(), dispatcherProvider),
        storageCleanup = cleanup,
        configurationRepository = configuration,
        appScope = appScope,
        dispatcherProvider = dispatcherProvider,
    )

    @Before
    fun setUp() {
        every { workManager.cancelUniqueWork(any()) } returns completedOperation
        every { workManager.enqueueUniquePeriodicWork(any(), any(), any<PeriodicWorkRequest>()) } returns completedOperation
    }

    @After
    fun tearDown() {
        appScope.cancel()
        database.close()
        context.deleteDatabase("app_history_v2.db")
        context.deleteDatabase("app_history.db")
        context.deleteDatabase("app_history_restore_staging.db")
    }

    @Test
    fun `Given recording turns off When time passes and packages change Then capture stops without deleting history`() = runTest {
        scheduler.start()
        runCurrent()
        val historyFile = context.getDatabasePath("app_history_v2.db")
        historyFile.parentFile!!.mkdirs()
        historyFile.writeText("previous history")

        configuration[ConfigurationKey.AppHistoryRecordingEnabled] = false
        runCurrent()
        advanceTimeBy(60_001)
        packageChanges.emit(PackageChangeEvent(installedApp.packageName, PackageChangeAction.Added))
        runCurrent()

        persistence.observe(installedApp.packageName).first().getOrThrow() shouldBe emptyList()
        historyFile.readText() shouldBe "previous history"
        packageChanges.subscriberCount shouldBe 0
        verify(exactly = 1) { workManager.cancelUniqueWork(any()) }
    }

    @Test
    fun `Given recording starts disabled When it turns on Then package changes are captured once`() = runTest {
        configuration[ConfigurationKey.AppHistoryRecordingEnabled] = false
        scheduler.start()
        scheduler.start()
        runCurrent()

        configuration[ConfigurationKey.AppHistoryRecordingEnabled] = true
        runCurrent()
        packageChanges.emit(PackageChangeEvent(installedApp.packageName, PackageChangeAction.Replaced))
        runCurrent()

        persistence.observe(installedApp.packageName).first().getOrThrow().map { it.record.packageName.value } shouldBe listOf("com.whatsapp")
        verify(exactly = 1) { workManager.enqueueUniquePeriodicWork(any(), any(), any<PeriodicWorkRequest>()) }
    }

    @Test
    fun `Given periodic cancellation fails When recording turns on Then configuration observation continues`() = runTest {
        configuration[ConfigurationKey.AppHistoryRecordingEnabled] = false
        every { workManager.cancelUniqueWork(any()) } returns failedOperation

        scheduler.start()
        runCurrent()
        configuration[ConfigurationKey.AppHistoryRecordingEnabled] = true
        runCurrent()
        packageChanges.emit(PackageChangeEvent(installedApp.packageName, PackageChangeAction.Replaced))
        runCurrent()

        persistence.observe(installedApp.packageName).first().getOrThrow().map { it.record.packageName.value } shouldBe listOf("com.whatsapp")
        packageChanges.subscriberCount shouldBe 1
        verify(exactly = 1) { workManager.enqueueUniquePeriodicWork(any(), any(), any<PeriodicWorkRequest>()) }
    }

    @Test
    fun `Given recording is enabled When unrelated configuration changes Then capture is not restarted`() = runTest {
        scheduler.start()
        runCurrent()

        configuration[ConfigurationKey.WhatChangedTabEnabled] = true
        runCurrent()

        packageChanges.subscriberCount shouldBe 1
        verify(exactly = 1) { workManager.enqueueUniquePeriodicWork(any(), any(), any<PeriodicWorkRequest>()) }
    }

    @Test
    fun `Given recording is disabled When starting Then only uncompressed history is deleted and capture stays off`() = runTest {
        configuration[ConfigurationKey.AppHistoryRecordingEnabled] = false
        val historyFile = context.getDatabasePath("app_history_v2.db")
        historyFile.parentFile!!.mkdirs()
        historyFile.writeText("previous history")
        context.getDatabasePath("app_history.db").writeText("legacy history")
        context.getDatabasePath("app_history_restore_staging.db").writeText("legacy staging")

        scheduler.start()
        runCurrent()
        advanceTimeBy(60_001)
        packageChanges.emit(PackageChangeEvent(installedApp.packageName, PackageChangeAction.Added))
        runCurrent()

        historyFile.readText() shouldBe "previous history"
        context.getDatabasePath("app_history.db").exists() shouldBe false
        context.getDatabasePath("app_history_restore_staging.db").exists() shouldBe false
        persistence.observe(installedApp.packageName).first().getOrThrow() shouldBe emptyList()
        verify(exactly = 0) { workManager.enqueueUniquePeriodicWork(any(), any(), any<PeriodicWorkRequest>()) }
        verify(exactly = 1) { workManager.cancelUniqueWork(any()) }
    }

    @Test
    fun `Given recording is disabled without existing files When starting Then no history database is created`() = runTest {
        configuration[ConfigurationKey.AppHistoryRecordingEnabled] = false

        scheduler.start()
        runCurrent()

        context.getDatabasePath("app_history_v2.db").exists() shouldBe false
    }
}
