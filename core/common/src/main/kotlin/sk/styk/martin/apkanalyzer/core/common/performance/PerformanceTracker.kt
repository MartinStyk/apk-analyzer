package sk.styk.martin.apkanalyzer.core.common.performance

interface PerformanceTracker {
    fun startTrace(name: String): PerformanceTrace
}

interface PerformanceTrace {
    fun putMetric(name: String, value: Long)

    fun putAttribute(name: String, value: String)

    fun stop()
}
