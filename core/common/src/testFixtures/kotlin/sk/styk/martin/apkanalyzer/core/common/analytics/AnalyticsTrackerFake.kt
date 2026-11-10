package sk.styk.martin.apkanalyzer.core.common.analytics

class AnalyticsTrackerFake(vararg events: AnalyticsEvent) : AnalyticsTracker {

    private val mutableEvents = events.toMutableList()
    val events: List<AnalyticsEvent>
        get() = mutableEvents.toList()

    private val mutableFormFactors = mutableListOf<AnalyticsFormFactor>()
    val formFactors: List<AnalyticsFormFactor>
        get() = mutableFormFactors.toList()

    override fun track(event: AnalyticsEvent) {
        mutableEvents += event
    }

    override fun setFormFactor(formFactor: AnalyticsFormFactor) {
        mutableFormFactors += formFactor
    }
}
