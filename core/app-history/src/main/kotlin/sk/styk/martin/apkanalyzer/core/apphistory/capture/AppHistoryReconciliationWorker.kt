package sk.styk.martin.apkanalyzer.core.apphistory.capture

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import sk.styk.martin.apkanalyzer.core.common.logger.Logger

@HiltWorker
internal class AppHistoryReconciliationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val captureRepository: AppHistoryCaptureRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Logger.i(APP_HISTORY, "Periodic reconciliation started")
        return captureRepository.reconcileAll()
            .onSuccess {
                Logger.i(APP_HISTORY, "Periodic reconciliation successful")
            }
            .onFailure {
                Logger.w(APP_HISTORY, it, "Periodic reconciliation failed")
            }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }
}
