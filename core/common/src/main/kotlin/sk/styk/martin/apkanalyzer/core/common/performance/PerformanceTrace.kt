package sk.styk.martin.apkanalyzer.core.common.performance

interface PerformanceTrace : AutoCloseable {
    fun putMetric(name: String, value: Long)

    fun putAttribute(name: String, value: String)
}
