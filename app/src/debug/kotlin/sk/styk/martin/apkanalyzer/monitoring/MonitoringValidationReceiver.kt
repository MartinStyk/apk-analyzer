package sk.styk.martin.apkanalyzer.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceAttributeName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceMetricName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTraceName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import javax.inject.Inject

@AndroidEntryPoint
internal class MonitoringValidationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var performanceTracker: PerformanceTracker

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getStringExtra(EXTRA_SIGNAL)) {
            SIGNAL_CRASH -> throw IllegalStateException("Intentional monitoring validation crash")

            SIGNAL_NON_FATAL -> Logger.e(
                TAG,
                IllegalStateException("Intentional monitoring validation non-fatal"),
                "Recording intentional monitoring validation non-fatal",
            )

            SIGNAL_PERFORMANCE -> performanceTracker.startTrace(PerformanceTraceName.MONITORING_VALIDATION).use { trace ->
                trace.putMetric(PerformanceMetricName.VALIDATION_US, 1L)
                trace.putAttribute(PerformanceAttributeName.OUTCOME, VALIDATION_ATTRIBUTE_VALUE)
            }

            else -> Logger.w(TAG, "Unknown monitoring validation signal")
        }
    }
}

private const val EXTRA_SIGNAL = "signal"
private const val SIGNAL_CRASH = "crash"
private const val SIGNAL_NON_FATAL = "non_fatal"
private const val SIGNAL_PERFORMANCE = "performance"
private const val VALIDATION_ATTRIBUTE_VALUE = "success"
private const val TAG = "MonitoringValidation"
