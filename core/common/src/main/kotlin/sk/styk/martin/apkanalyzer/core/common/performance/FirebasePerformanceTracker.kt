package sk.styk.martin.apkanalyzer.core.common.performance

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirebasePerformanceTracker
@Inject
constructor() : PerformanceTracker {
    override fun startTrace(name: String): PerformanceTrace {
        val trace = FirebasePerformance.getInstance().newTrace(name)
        trace.start()
        return FirebasePerformanceTrace(trace)
    }
}

private class FirebasePerformanceTrace(private val trace: Trace) : PerformanceTrace {
    override fun putMetric(name: String, value: Long) {
        trace.putMetric(name, value)
    }

    override fun putAttribute(name: String, value: String) {
        trace.putAttribute(name, value)
    }

    override fun close() {
        trace.stop()
    }
}
