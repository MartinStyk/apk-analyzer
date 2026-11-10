package sk.styk.martin.apkanalyzer.feature.appdetail.domain.analytics

import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryChangeCategory
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsEvent
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsParameterName
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsTracker
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import javax.inject.Inject

class AppDetailAnalytics @Inject constructor(private val analyticsTracker: AnalyticsTracker) {

    fun track(event: AppDetailAnalyticsEvent) {
        analyticsTracker.track(
            when (event) {
                is AppDetailAnalyticsEvent.Opened -> AnalyticsEvent(
                    "app_detail_opened",
                    mapOf(AnalyticsParameterName.AnalysisMode to event.input.analyticsAnalysisMode),
                )

                is AppDetailAnalyticsEvent.SectionOpened -> AnalyticsEvent(
                    "app_detail_section_opened",
                    mapOf(
                        AnalyticsParameterName.Section to event.section.analyticsValue,
                        AnalyticsParameterName.AnalysisMode to event.input.analyticsAnalysisMode,
                    ),
                )

                is AppDetailAnalyticsEvent.ActionPerformed -> AnalyticsEvent(
                    "app_detail_action_performed",
                    mapOf(
                        AnalyticsParameterName.Action to event.action.analyticsValue,
                        AnalyticsParameterName.AnalysisMode to event.input.analyticsAnalysisMode,
                    ),
                )

                is AppDetailAnalyticsEvent.HistoryTimelineOpened -> AnalyticsEvent(
                    "app_history_timeline_opened",
                    mapOf(
                        AnalyticsParameterName.HistoryBucket to event.bucket.analyticsValue,
                        AnalyticsParameterName.ChangeCategories to event.categories.analyticsValue,
                    ),
                )

                is AppDetailAnalyticsEvent.HistoryChangeOpened -> AnalyticsEvent(
                    "app_history_change_opened",
                    mapOf(AnalyticsParameterName.ChangeCategories to event.categories.analyticsValue),
                )
            },
        )
    }
}

private val AppDetailInput.analyticsAnalysisMode: String
    get() = when (this) {
        is AppDetailInput.ApkFile -> "apk_file"
        is AppDetailInput.InstalledPackage -> "installed_package"
        is AppDetailInput.Historical -> "historical"
    }

private val AppDetailAnalyticsEvent.HistoryBucket.analyticsValue: String
    get() = when (this) {
        AppDetailAnalyticsEvent.HistoryBucket.Empty -> "empty"
        AppDetailAnalyticsEvent.HistoryBucket.FirstOnly -> "first_only"
        AppDetailAnalyticsEvent.HistoryBucket.OneToFive -> "1_5"
        AppDetailAnalyticsEvent.HistoryBucket.SixOrMore -> "6_plus"
    }

private val Set<AppHistoryChangeCategory>.analyticsValue: String
    get() = map { it.analyticsValue }.sorted().joinToString(",").ifEmpty { "none" }

private val AppHistoryChangeCategory.analyticsValue: String
    get() = when (this) {
        AppHistoryChangeCategory.Version -> "version"
        AppHistoryChangeCategory.SdkLevels -> "sdk_levels"
        AppHistoryChangeCategory.Size -> "size"
        AppHistoryChangeCategory.InstallSource -> "install_source"
        AppHistoryChangeCategory.ManifestFlags -> "manifest_flags"
        AppHistoryChangeCategory.Permissions -> "permissions"
        AppHistoryChangeCategory.Components -> "components"
        AppHistoryChangeCategory.Signing -> "signing"
        AppHistoryChangeCategory.NativeLibraries -> "native_libraries"
        AppHistoryChangeCategory.Requirements -> "requirements"
    }

private val AppDetailAnalyticsEvent.Section.analyticsValue: String
    get() = when (this) {
        AppDetailAnalyticsEvent.Section.Manifest -> "manifest"
        AppDetailAnalyticsEvent.Section.General -> "general"
        AppDetailAnalyticsEvent.Section.Permissions -> "permissions"
        AppDetailAnalyticsEvent.Section.Components -> "components"
        AppDetailAnalyticsEvent.Section.Activities -> "activities"
        AppDetailAnalyticsEvent.Section.Services -> "services"
        AppDetailAnalyticsEvent.Section.Receivers -> "receivers"
        AppDetailAnalyticsEvent.Section.Providers -> "providers"
        AppDetailAnalyticsEvent.Section.Certificates -> "certificates"
        AppDetailAnalyticsEvent.Section.Features -> "features"
    }

private val AppDetailAnalyticsEvent.Action.analyticsValue: String
    get() = when (this) {
        AppDetailAnalyticsEvent.Action.ExportApk -> "export_apk"
        AppDetailAnalyticsEvent.Action.ExportIcon -> "export_icon"
        AppDetailAnalyticsEvent.Action.ViewSummary -> "view_summary"
        AppDetailAnalyticsEvent.Action.CopySummary -> "copy_summary"
        AppDetailAnalyticsEvent.Action.ShareSummary -> "share_summary"
        AppDetailAnalyticsEvent.Action.ExportInventory -> "export_inventory"
        AppDetailAnalyticsEvent.Action.OpenPlayStore -> "open_play_store"
        AppDetailAnalyticsEvent.Action.OpenAppInfo -> "open_app_info"
        AppDetailAnalyticsEvent.Action.OpenHistory -> "open_history"
        AppDetailAnalyticsEvent.Action.LaunchComponent -> "launch_component"
    }
