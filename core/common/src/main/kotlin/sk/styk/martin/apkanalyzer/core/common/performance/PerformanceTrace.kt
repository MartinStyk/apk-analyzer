package sk.styk.martin.apkanalyzer.core.common.performance

interface PerformanceTrace : AutoCloseable {
    operator fun set(name: String, value: Long)

    operator fun set(name: String, value: String)
}
