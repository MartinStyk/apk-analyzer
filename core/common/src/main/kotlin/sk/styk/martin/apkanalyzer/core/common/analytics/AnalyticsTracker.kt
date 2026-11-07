package sk.styk.martin.apkanalyzer.core.common.analytics

public interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)

    fun setFormFactor(formFactor: AnalyticsFormFactor)
}

public enum class AnalyticsFormFactor(val value: String) {
    Phone("phone"),
    Tv("tv"),
}

public data class AnalyticsEvent(val name: String, val parameters: Map<AnalyticsParameterName, String> = emptyMap())

public enum class AnalyticsParameterName(val value: String) {
    Action("action"),
    AnalysisMode("analysis_mode"),
    Dimension("dimension"),
    Section("section"),
}
