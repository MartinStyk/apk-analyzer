package sk.styk.martin.apkanalyzer.core.apphistory.impl.capture

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apphistory.api.capture.AppHistoryCaptureRepository
import sk.styk.martin.apkanalyzer.core.apphistory.impl.restore.AppHistoryRestoreMerger
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryStorageCleanup
import sk.styk.martin.apkanalyzer.core.apps.api.PackageChangeAction
import sk.styk.martin.apkanalyzer.core.apps.api.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.common.configuration.ConfigurationKey
import sk.styk.martin.apkanalyzer.core.common.configuration.ConfigurationRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

private const val PERIODIC_RECONCILIATION_WORK_NAME = "app_history_periodic_reconciliation"

@Singleton
internal class AppHistoryCaptureSchedulerImpl @Inject constructor(
    private val captureRepository: AppHistoryCaptureRepository,
    private val packageChangesObserver: PackageChangesObserver,
    private val workManager: Lazy<WorkManager>,
    private val restoreMerger: AppHistoryRestoreMerger,
    private val storageCleanup: AppHistoryStorageCleanup,
    private val configurationRepository: ConfigurationRepository,
    private val appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
) : AppHistoryCaptureScheduler,
    DefaultLifecycleObserver {

    private val started = AtomicBoolean(false)

    override fun onCreate(owner: LifecycleOwner) {
        start()
    }

    override fun start() {
        if (!started.compareAndSet(false, true)) return

        appScope.launch(dispatcherProvider.io()) {
            storageCleanup.deleteUncompressedDatabases()

            configurationRepository.observe(ConfigurationKey.AppHistoryRecordingEnabled)
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    if (enabled) {
                        recordHistory()
                    } else {
                        cancelPeriodicReconciliation()
                    }
                }
        }
    }

    private suspend fun recordHistory(): Unit = coroutineScope {
        launch {
            restoreMerger.mergeIfPending()
        }

        launch {
            delay(1.minutes)
            captureRepository.reconcileAll()
                .onFailure { Logger.w(APP_HISTORY, it, "Reconciliation sweep failed") }
        }

        packageChangesObserver.observe()
            .onEach { event ->
                if (event.action != PackageChangeAction.Removed) {
                    captureRepository.reconcile(event.packageName)
                        .onFailure { Logger.w(APP_HISTORY, it, "Fast-path capture failed for ${event.packageName.value}") }
                }
            }
            .launchIn(this)

        schedulePeriodicReconciliation()
    }

    private fun schedulePeriodicReconciliation() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<AppHistoryReconciliationWorker>(
            repeatInterval = 7,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
        ).setConstraints(constraints).build()

        workManager.get().enqueueUniquePeriodicWork(
            uniqueWorkName = PERIODIC_RECONCILIATION_WORK_NAME,
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
            request = request,
        )
    }

    private suspend fun cancelPeriodicReconciliation() {
        runCatchingCancellable {
            workManager.get().cancelUniqueWork(PERIODIC_RECONCILIATION_WORK_NAME).await()
        }.onFailure { Logger.w(APP_HISTORY, it, "Periodic history cancellation failed") }
    }
}
