package sk.styk.martin.apkanalyzer.core.common.analytics

public interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

public data class AnalyticsEvent(
    val name: String,
    val parameters: Map<String, String> = emptyMap(),
)
