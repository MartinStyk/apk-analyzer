package sk.styk.martin.apkanalyzer.core.apps.api.filtering

@JvmInline
value class AppFilterScope(val id: String) {
    companion object {
        val List = AppFilterScope("list")
    }
}
