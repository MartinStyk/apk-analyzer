package sk.styk.martin.apkanalyzer.feature.appdetail.phone

import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsEvent
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsParameterName
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsTracker
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import javax.inject.Inject

internal class AppDetailAnalytics @Inject constructor(private val analyticsTracker: AnalyticsTracker) {

    fun track(event: AppDetailAnalyticsEvent) {
        analyticsTracker.track(
            when (event) {
                is AppDetailAnalyticsEvent.Opened -> AnalyticsEvent(
                    "app_detail_opened",
                    mapOf(AnalyticsParameterName.AnalysisMode to event.input.analyticsAnalysisMode),
                )

                is AppDetailAnalyticsEvent.SectionOpened -> AnalyticsEvent(
                    "app_detail_section_opened",
                    mapOf(AnalyticsParameterName.Section to event.section.analyticsValue),
                )

                is AppDetailAnalyticsEvent.ActionPerformed -> AnalyticsEvent(
                    "app_detail_action_performed",
                    mapOf(AnalyticsParameterName.Action to event.action.analyticsValue),
                )

                is AppDetailAnalyticsEvent.HistoricalSectionOpened -> AnalyticsEvent(
                    "app_history_section_opened",
                    mapOf(AnalyticsParameterName.Section to event.section.analyticsValue),
                )

                is AppDetailAnalyticsEvent.HistoryTimelineOpened -> AnalyticsEvent(
                    "app_history_timeline_opened",
                    mapOf(AnalyticsParameterName.HistoryBucket to event.bucket.analyticsValue),
                )

                is AppDetailAnalyticsEvent.HistoryChangeOpened -> AnalyticsEvent(
                    "app_history_change_opened",
                    mapOf(AnalyticsParameterName.ChangeKind to event.kind.analyticsValue),
                )

                AppDetailAnalyticsEvent.HistoryFullDetailOpened -> AnalyticsEvent("app_history_full_detail_opened")
            },
        )
    }
}

internal sealed interface AppDetailAnalyticsEvent {
    data class Opened(val input: AppDetailInput) : AppDetailAnalyticsEvent

    data class SectionOpened(val section: Section) : AppDetailAnalyticsEvent

    data class ActionPerformed(val action: Action) : AppDetailAnalyticsEvent

    data class HistoricalSectionOpened(val section: Section) : AppDetailAnalyticsEvent

    data class HistoryTimelineOpened(val bucket: HistoryBucket) : AppDetailAnalyticsEvent

    data class HistoryChangeOpened(val kind: AppHistoryEntry.Change.Kind) : AppDetailAnalyticsEvent

    data object HistoryFullDetailOpened : AppDetailAnalyticsEvent

    enum class HistoryBucket {
        Empty,
        FirstOnly,
        OneToFive,
        SixOrMore,
    }

    enum class Section {
        Manifest,
        General,
        Permissions,
        Components,
        Activities,
        Services,
        Receivers,
        Providers,
        Certificates,
        Features,
    }

    enum class Action {
        ExportApk,
        ExportIcon,
        ViewSummary,
        CopySummary,
        ShareSummary,
        ExportInventory,
        OpenPlayStore,
        OpenAppInfo,
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

private val AppHistoryEntry.Change.Kind.analyticsValue: String
    get() = when (this) {
        AppHistoryEntry.Change.Kind.Updated -> "updated"
        AppHistoryEntry.Change.Kind.Reinstalled -> "reinstalled"
        AppHistoryEntry.Change.Kind.NewDevice -> "new_device"
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
    }
