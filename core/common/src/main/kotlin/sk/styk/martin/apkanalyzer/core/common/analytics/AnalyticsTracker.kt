package sk.styk.martin.apkanalyzer.core.common.analytics

public interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

public data class AnalyticsEvent(
    val name: String,
    val parameters: Map<AnalyticsParameterName, String> = emptyMap(),
)

public enum class AnalyticsParameterName(
    internal val value: String,
) {
    Action("action"),
    AnalysisMode("analysis_mode"),
    Dimension("dimension"),
    Section("section"),
}
