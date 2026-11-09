package sk.styk.martin.apkanalyzer.feature.appdetail.phone.analytics

import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryChangeCategory
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput

internal sealed interface AppDetailAnalyticsEvent {
    data class Opened(val input: AppDetailInput) : AppDetailAnalyticsEvent

    data class SectionOpened(val section: Section, val input: AppDetailInput) : AppDetailAnalyticsEvent

    data class ActionPerformed(val action: Action, val input: AppDetailInput) : AppDetailAnalyticsEvent

    data class HistoryTimelineOpened(val bucket: HistoryBucket, val categories: Set<AppHistoryChangeCategory>) : AppDetailAnalyticsEvent

    data class HistoryChangeOpened(val categories: Set<AppHistoryChangeCategory>) : AppDetailAnalyticsEvent

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
        OpenHistory,
    }
}
