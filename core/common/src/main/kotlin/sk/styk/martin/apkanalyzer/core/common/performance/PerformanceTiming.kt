package sk.styk.martin.apkanalyzer.core.common.performance

import android.os.SystemClock

fun interface MonotonicClock {
    fun nowNanos(): Long
}

data object ElapsedRealtimeMonotonicClock : MonotonicClock {
    override fun nowNanos(): Long = SystemClock.elapsedRealtimeNanos()
}

fun <T> PerformanceTrace.measureStage(
    metricName: String,
    clock: MonotonicClock = ElapsedRealtimeMonotonicClock,
    block: () -> T,
): T = StageMeasurement(this, metricName, clock).use { block() }

suspend fun <T> PerformanceTrace.measureSuspendStage(
    metricName: String,
    clock: MonotonicClock = ElapsedRealtimeMonotonicClock,
    block: suspend () -> T,
): T = StageMeasurement(this, metricName, clock).use { block() }

private class StageMeasurement(
    private val trace: PerformanceTrace,
    private val metricName: String,
    private val clock: MonotonicClock,
) : AutoCloseable {
    private val startedAtNanos = clock.nowNanos()

    override fun close() {
        trace.putMetric(metricName, (clock.nowNanos() - startedAtNanos) / 1_000L)
    }
}
