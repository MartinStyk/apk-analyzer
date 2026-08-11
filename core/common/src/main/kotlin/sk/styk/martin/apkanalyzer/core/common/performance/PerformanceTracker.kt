package sk.styk.martin.apkanalyzer.core.common.performance

interface PerformanceTracker {
    fun startTrace(name: String): PerformanceTrace
}

interface PerformanceTrace : AutoCloseable {
    fun putMetric(name: String, value: Long)

    fun putAttribute(name: String, value: String)
}
