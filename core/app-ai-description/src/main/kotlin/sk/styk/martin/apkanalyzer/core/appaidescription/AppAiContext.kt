package sk.styk.martin.apkanalyzer.core.appaidescription

internal const val MAX_CONTEXT_SIZE_CHARS = 8_000

internal data class AppAiContext(
    val packageName: String,
    val appName: String,
    val targetSdk: Int?,
    val permissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
)

internal fun AppAiContext.serialize(): String = buildString {
    appendLine("packageName=$packageName")
    appendLine("appName=$appName")
    appendLine("targetSdk=${targetSdk ?: "unknown"}")
    appendLine("permissions=${permissions.joinToString(",")}")
    appendLine("activities=${activities.joinToString(",")}")
    appendLine("services=${services.joinToString(",")}")
    appendLine("receivers=${receivers.joinToString(",")}")
}.take(MAX_CONTEXT_SIZE_CHARS)

internal fun AppAiContext.identifiers(): List<String> = buildList {
    add(packageName)
    addAll(permissions)
    addAll(activities)
    addAll(services)
    addAll(receivers)
}
