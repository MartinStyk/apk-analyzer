package sk.styk.martin.apkanalyzer.core.common.analytics

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)

    fun setFormFactor(formFactor: AnalyticsFormFactor)
}

enum class AnalyticsFormFactor(val value: String) {
    Phone("phone"),
    Tv("tv"),
}

data class AnalyticsEvent(val name: String, val parameters: Map<AnalyticsParameterName, String> = emptyMap())

enum class AnalyticsParameterName(val value: String) {
    Action("action"),
    AnalysisMode("analysis_mode"),
    ChangeCategories("change_categories"),
    HistoryBucket("history_bucket"),
    Dimension("dimension"),
    Section("section"),
}
