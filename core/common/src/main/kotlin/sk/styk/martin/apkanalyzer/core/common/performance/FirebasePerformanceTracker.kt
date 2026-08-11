package sk.styk.martin.apkanalyzer.core.common.performance

import com.google.firebase.perf.FirebasePerformance
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
