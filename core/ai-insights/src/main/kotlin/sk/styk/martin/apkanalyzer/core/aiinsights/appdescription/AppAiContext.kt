package sk.styk.martin.apkanalyzer.core.aiinsights.appdescription

internal data class AppAiContext(
    val packageName: String,
    val appName: String,
    val targetSdk: Int?,
    val permissions: List<String>,
)

internal fun AppAiContext.identifiers(): List<String> = buildList {
    add(packageName)
    addAll(permissions)
}
