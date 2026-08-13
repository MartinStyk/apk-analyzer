package sk.styk.martin.apkanalyzer.core.common.performance

import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import kotlin.time.measureTimedValue

interface PerformanceTrace : AutoCloseable {
    operator fun set(name: String, value: Number)

    operator fun set(name: String, value: String)
}

inline fun <T> PerformanceTrace.measuredSection(metric: String, block: () -> T): T {
    val (value, duration) = measureTimedValue(block)
    this[metric] = duration.inWholeMilliseconds
    return value
}

inline fun <T> PerformanceTrace.timedSection(
    tag: String,
    operation: String,
    metric: String,
    context: String = "",
    block: () -> T,
): T {
    val suffix = if (context.isEmpty()) "" else ": $context"
    Logger.d(tag, "$operation started$suffix")
    val result = measuredSection(metric, block)
    Logger.d(tag, "$operation finished$suffix")
    return result
}

var PerformanceTrace.outcome: TraceOutcome
    get() = throw UnsupportedOperationException("PerformanceTrace.outcome is write-only")
    set(value) {
        this["outcome"] = value.attributeValue
    }

var PerformanceTrace.permission: TracePermission
    get() = throw UnsupportedOperationException("PerformanceTrace.permission is write-only")
    set(value) {
        this["permission"] = value.attributeValue
    }

var PerformanceTrace.analysisMode: String
    get() = throw UnsupportedOperationException("PerformanceTrace.analysisMode is write-only")
    set(value) {
        this["analysis_mode"] = value
    }

var PerformanceTrace.appCount: Int
    get() = throw UnsupportedOperationException("PerformanceTrace.appCount is write-only")
    set(value) {
        this["app_count"] = value
    }

enum class TraceOutcome(internal val attributeValue: String) {
    Success("success"),
    Degraded("degraded"),
    Error("error"),
    Cancelled("cancelled"),
}

enum class TracePermission(internal val attributeValue: String) {
    Granted("granted"),
    Denied("denied"),
}
