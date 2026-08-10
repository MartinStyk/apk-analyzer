package sk.styk.martin.apkanalyzer.core.common.performance

import android.os.SystemClock

fun interface MonotonicClock {
    fun nowNanos(): Long
}

data object ElapsedRealtimeMonotonicClock : MonotonicClock {
    override fun nowNanos(): Long = SystemClock.elapsedRealtimeNanos()
}

inline fun <T> PerformanceTrace.measureStage(
    metricName: String,
    clock: MonotonicClock = ElapsedRealtimeMonotonicClock,
    block: () -> T,
): T {
    val startedAtNanos = clock.nowNanos()
    return try {
        block()
    } finally {
        putMetric(metricName, (clock.nowNanos() - startedAtNanos) / 1_000L)
    }
}

suspend inline fun <T> PerformanceTrace.measureSuspendStage(
    metricName: String,
    clock: MonotonicClock = ElapsedRealtimeMonotonicClock,
    block: suspend () -> T,
): T {
    val startedAtNanos = clock.nowNanos()
    return try {
        block()
    } finally {
        putMetric(metricName, (clock.nowNanos() - startedAtNanos) / 1_000L)
    }
}
