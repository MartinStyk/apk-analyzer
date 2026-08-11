package sk.styk.martin.apkanalyzer.core.common.logger

import java.util.concurrent.atomic.AtomicLong

private val requestSequence = AtomicLong(0)

class LogRequest {
    internal val id = requestSequence.incrementAndGet()
}

sealed interface LogEvent {
    data class ScreenOpen(val key: Any) : LogEvent

    data class Operation(
        val name: String,
        val request: LogRequest,
        val state: State,
        val stage: String? = null,
        val context: String? = null,
    ) : LogEvent {
        enum class State(val value: String) {
            Started("started"),
            Succeeded("succeeded"),
            Degraded("degraded"),
            Failed("failed"),
        }
    }
}
