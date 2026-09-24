package sk.styk.martin.apkanalyzer.core.apphistory.capture

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import sk.styk.martin.apkanalyzer.core.apphistory.restore.AppHistoryRestoreMerger
import sk.styk.martin.apkanalyzer.core.apps.PackageChangeAction
import sk.styk.martin.apkanalyzer.core.apps.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
@Singleton
internal class AppHistoryCaptureSchedulerImpl @Inject constructor(
    private val captureRepository: AppHistoryCaptureRepository,
    private val packageChangesObserver: PackageChangesObserver,
    private val workManager: Lazy<WorkManager>,
    private val restoreMerger: AppHistoryRestoreMerger,
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

        appScope.launch(dispatcherProvider.default()) {
            restoreMerger.mergeIfPending()
        }

        appScope.launch(dispatcherProvider.default()) {
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
            .launchIn(appScope + dispatcherProvider.default())

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
            uniqueWorkName = "app_history_periodic_reconciliation",
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
            request = request,
        )
    }
}
