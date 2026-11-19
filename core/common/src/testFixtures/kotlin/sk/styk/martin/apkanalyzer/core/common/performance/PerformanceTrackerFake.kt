package sk.styk.martin.apkanalyzer.core.common.performance

class PerformanceTrackerFake : PerformanceTracker {
    override fun startTrace(name: String): PerformanceTrace = PerformanceTraceFake()
}

private class PerformanceTraceFake : PerformanceTrace {
    override fun set(name: String, value: Number) = Unit

    override fun set(name: String, value: String) = Unit

    override fun close() = Unit
}
