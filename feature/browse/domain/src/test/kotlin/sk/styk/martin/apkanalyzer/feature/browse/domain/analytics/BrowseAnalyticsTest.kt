package sk.styk.martin.apkanalyzer.feature.browse.domain.analytics

import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsEvent
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsParameterName
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsTrackerFake

class BrowseAnalyticsTest {

    private val tracker = AnalyticsTrackerFake()
    private val analytics = BrowseAnalytics(tracker)

    @Test
    fun `Given browse selections When tracking Then dimensions and categories stay distinct`() {
        val dimension = BrowseAnalyticsEvent.DimensionOpened(BrowseDimension.SigningCertificate)
        val category = BrowseAnalyticsEvent.CategoryOpened(BrowseDimension.InstallSource)

        analytics.track(BrowseAnalyticsEvent.TabOpened)
        analytics.track(dimension)
        analytics.track(category)

        tracker.events shouldBe listOf(
            AnalyticsEvent("browse_tab_opened"),
            AnalyticsEvent("browse_dimension_opened", mapOf(AnalyticsParameterName.Dimension to "signing_certificate")),
            AnalyticsEvent("browse_category_opened", mapOf(AnalyticsParameterName.Dimension to "install_source")),
        )
    }
}
