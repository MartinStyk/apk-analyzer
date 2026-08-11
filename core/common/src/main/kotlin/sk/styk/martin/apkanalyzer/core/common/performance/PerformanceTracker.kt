package sk.styk.martin.apkanalyzer.core.common.performance

interface PerformanceTracker {
    fun <T> startTrace(name: String, block: (PerformanceTrace) -> T): T
}

interface PerformanceTrace {
    fun putMetric(name: String, value: Long)

    fun putAttribute(name: String, value: String)
}
