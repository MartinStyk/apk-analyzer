package sk.styk.martin.apkanalyzer.core.common.performance

import kotlin.coroutines.cancellation.CancellationException

interface PerformanceTracker {
    fun startTrace(name: String): PerformanceTrace
}

suspend fun <T> PerformanceTracker.startCancellableTrace(name: String, block: suspend (PerformanceTrace) -> T): T = startTrace(name).use { trace ->
    try {
        block(trace)
    } catch (cancellation: CancellationException) {
        val outcomeFailure = runCatching {
            trace[ATTRIBUTE_OUTCOME] = OUTCOME_CANCELLED
        }.exceptionOrNull()
        if (outcomeFailure != null && outcomeFailure !== cancellation) {
            cancellation.addSuppressed(outcomeFailure)
        }
        throw cancellation
    }
}

private const val ATTRIBUTE_OUTCOME = "outcome"
private const val OUTCOME_CANCELLED = "cancelled"
