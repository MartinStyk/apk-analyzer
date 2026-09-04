package sk.styk.martin.apkanalyzer.core.apphistory.capture

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import sk.styk.martin.apkanalyzer.core.apps.PackageChangeAction
import sk.styk.martin.apkanalyzer.core.apps.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

private val RECONCILIATION_START_DELAY = 30.seconds

@Singleton
internal class AppHistoryCaptureSchedulerImpl @Inject constructor(
    private val captureRepository: AppHistoryCaptureRepository,
    private val packageChangesObserver: PackageChangesObserver,
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
            delay(RECONCILIATION_START_DELAY)
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
    }
}
