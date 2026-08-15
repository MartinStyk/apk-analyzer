package sk.styk.martin.apkanalyzer.core.common.analytics

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

data class AnalyticsEvent(
    val name: String,
    val parameters: Map<String, String> = emptyMap(),
)
