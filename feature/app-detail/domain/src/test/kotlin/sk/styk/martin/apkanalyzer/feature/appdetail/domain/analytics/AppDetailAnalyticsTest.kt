package sk.styk.martin.apkanalyzer.feature.appdetail.domain.analytics

import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryChangeCategory
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsEvent
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsParameterName
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsTrackerFake
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.time.Instant

class AppDetailAnalyticsTest {

    private val tracker = AnalyticsTrackerFake()
    private val analytics = AppDetailAnalytics(tracker)

    @Test
    fun `Given different detail inputs When tracking opens Then analysis modes stay distinct`() {
        val installed = AppDetailInput.InstalledPackage("com.whatsapp")
        val apkFile = AppDetailInput.ApkFile("whatsapp.apk")
        val historical = AppDetailInput.Historical("com.whatsapp", 1, Instant.EPOCH)

        analytics.track(AppDetailAnalyticsEvent.Opened(installed))
        analytics.track(AppDetailAnalyticsEvent.Opened(apkFile))
        analytics.track(AppDetailAnalyticsEvent.Opened(historical))

        tracker.events shouldBe listOf(
            AnalyticsEvent("app_detail_opened", mapOf(AnalyticsParameterName.AnalysisMode to "installed_package")),
            AnalyticsEvent("app_detail_opened", mapOf(AnalyticsParameterName.AnalysisMode to "apk_file")),
            AnalyticsEvent("app_detail_opened", mapOf(AnalyticsParameterName.AnalysisMode to "historical")),
        )
    }

    @Test
    fun `Given installed detail interactions When tracking Then sections and actions keep their analysis mode`() {
        val input = AppDetailInput.InstalledPackage("com.whatsapp")

        analytics.track(AppDetailAnalyticsEvent.SectionOpened(AppDetailAnalyticsEvent.Section.Features, input))
        analytics.track(AppDetailAnalyticsEvent.ActionPerformed(AppDetailAnalyticsEvent.Action.OpenAppInfo, input))
        analytics.track(AppDetailAnalyticsEvent.ActionPerformed(AppDetailAnalyticsEvent.Action.OpenPlayStore, input))
        analytics.track(AppDetailAnalyticsEvent.ActionPerformed(AppDetailAnalyticsEvent.Action.LaunchComponent, input))

        tracker.events shouldBe listOf(
            AnalyticsEvent(
                "app_detail_section_opened",
                mapOf(AnalyticsParameterName.Section to "features", AnalyticsParameterName.AnalysisMode to "installed_package"),
            ),
            AnalyticsEvent(
                "app_detail_action_performed",
                mapOf(AnalyticsParameterName.Action to "open_app_info", AnalyticsParameterName.AnalysisMode to "installed_package"),
            ),
            AnalyticsEvent(
                "app_detail_action_performed",
                mapOf(AnalyticsParameterName.Action to "open_play_store", AnalyticsParameterName.AnalysisMode to "installed_package"),
            ),
            AnalyticsEvent(
                "app_detail_action_performed",
                mapOf(AnalyticsParameterName.Action to "launch_component", AnalyticsParameterName.AnalysisMode to "installed_package"),
            ),
        )
    }

    @Test
    fun `Given phone history events When tracking Then buckets and sorted categories are preserved`() {
        val categories = setOf(AppHistoryChangeCategory.Version, AppHistoryChangeCategory.Permissions)
        val timeline = AppDetailAnalyticsEvent.HistoryTimelineOpened(AppDetailAnalyticsEvent.HistoryBucket.SixOrMore, categories)

        analytics.track(timeline)
        analytics.track(AppDetailAnalyticsEvent.HistoryChangeOpened(emptySet()))

        tracker.events shouldBe listOf(
            AnalyticsEvent(
                "app_history_timeline_opened",
                mapOf(AnalyticsParameterName.HistoryBucket to "6_plus", AnalyticsParameterName.ChangeCategories to "permissions,version"),
            ),
            AnalyticsEvent("app_history_change_opened", mapOf(AnalyticsParameterName.ChangeCategories to "none")),
        )
    }
}
