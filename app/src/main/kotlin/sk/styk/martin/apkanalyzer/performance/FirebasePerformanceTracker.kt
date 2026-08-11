package sk.styk.martin.apkanalyzer.performance

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTrace
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirebasePerformanceTracker
@Inject
constructor() : PerformanceTracker {
    override fun <T> startTrace(name: String, block: (PerformanceTrace) -> T): T {
        val trace = runFirebasePerformanceOperation("start trace name=$name") {
            FirebasePerformance.getInstance().newTrace(name).apply(Trace::start)
        }
        val performanceTrace: StoppableTrace = if (trace == null) NoOpPerformanceTrace else FirebasePerformanceTrace(trace)
        return try {
            block(performanceTrace)
        } finally {
            performanceTrace.stop()
        }
    }
}

private interface StoppableTrace : PerformanceTrace {
    fun stop()
}

private class FirebasePerformanceTrace(private val trace: Trace) : StoppableTrace {
    private val lock = Any()
    private var isStopped = false

    override fun putMetric(name: String, value: Long) {
        synchronized(lock) {
            if (isStopped) return
            runFirebasePerformanceOperation("record metric name=$name") {
                trace.putMetric(name, value)
            }
        }
    }

    override fun putAttribute(name: String, value: String) {
        synchronized(lock) {
            if (isStopped) return
            runFirebasePerformanceOperation("record attribute name=$name") {
                trace.putAttribute(name, value)
            }
        }
    }

    override fun stop() {
        synchronized(lock) {
            if (isStopped) return
            isStopped = true
            runFirebasePerformanceOperation("stop trace") {
                trace.stop()
            }
        }
    }
}

private data object NoOpPerformanceTrace : StoppableTrace {
    override fun putMetric(name: String, value: Long) = Unit

    override fun putAttribute(name: String, value: String) = Unit

    override fun stop() = Unit
}

private inline fun <T> runFirebasePerformanceOperation(operation: String, block: () -> T): T? = try {
    block()
} catch (exception: RuntimeException) {
    Logger.w(TAG, exception, "Firebase Performance failed to $operation")
    null
}

private const val TAG = "FirebasePerformance"
