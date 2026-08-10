package sk.styk.martin.apkanalyzer.core.common.logger

import java.util.concurrent.atomic.AtomicLong

private val requestSequence = AtomicLong(0)

fun nextOperationRequest(): Long = requestSequence.incrementAndGet()

fun operationLogMessage(
    operation: String,
    request: Long,
    event: String,
    stage: String? = null,
    context: String? = null,
): String {
    val builder = StringBuilder("operation=")
        .append(operation)
        .append(" request=")
        .append(request)
    if (stage != null) {
        builder.append(" stage=").append(stage)
    }
    builder.append(" event=").append(event)
    if (context != null) {
        builder.append(' ').append(context)
    }
    return builder.toString()
}
