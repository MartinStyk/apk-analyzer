package sk.styk.martin.apkanalyzer.core.common.performance

interface PerformanceTracker {
    fun startTrace(name: String): PerformanceTrace
}
