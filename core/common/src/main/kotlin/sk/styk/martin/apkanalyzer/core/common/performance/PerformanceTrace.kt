package sk.styk.martin.apkanalyzer.core.common.performance

interface PerformanceTrace : AutoCloseable {
    operator fun set(name: String, value: Long)

    operator fun set(name: String, value: String)

    fun setOutcome(outcome: TraceOutcome) {
        this["outcome"] = outcome.attributeValue
    }

    fun setPermission(permission: TracePermission) {
        this["permission"] = permission.attributeValue
    }
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
