package sk.styk.martin.apkanalyzer.core.common.performance

import com.google.firebase.perf.metrics.Trace

internal class FirebasePerformanceTrace(private val trace: Trace) : PerformanceTrace {
    override fun set(name: String, value: Long) {
        trace.putMetric(name, value)
    }

    override fun set(name: String, value: String) {
        trace.putAttribute(name, value)
    }

    override fun close() {
        trace.stop()
    }
}
