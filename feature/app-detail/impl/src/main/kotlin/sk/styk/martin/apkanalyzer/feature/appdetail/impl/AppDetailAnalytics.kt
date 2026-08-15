package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsEvent
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsTracker
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import javax.inject.Inject

internal class AppDetailAnalytics @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) {

    fun track(event: AppDetailAnalyticsEvent) {
        analyticsTracker.track(
            when (event) {
                is AppDetailAnalyticsEvent.Opened -> AnalyticsEvent(
                    EVENT_APP_DETAIL_OPENED,
                    mapOf(PARAMETER_ANALYSIS_MODE to event.appReference.analyticsAnalysisMode),
                )
                is AppDetailAnalyticsEvent.SectionOpened -> AnalyticsEvent(
                    EVENT_SECTION_OPENED,
                    mapOf(PARAMETER_SECTION to event.section.analyticsValue),
                )
                is AppDetailAnalyticsEvent.ActionPerformed -> AnalyticsEvent(
                    EVENT_ACTION_PERFORMED,
                    mapOf(PARAMETER_ACTION to event.action.analyticsValue),
                )
            },
        )
    }
}

internal sealed interface AppDetailAnalyticsEvent {
    data class Opened(val appReference: AppReference) : AppDetailAnalyticsEvent

    data class SectionOpened(val section: Section) : AppDetailAnalyticsEvent

    data class ActionPerformed(val action: Action) : AppDetailAnalyticsEvent

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
    }
}

private const val EVENT_APP_DETAIL_OPENED = "app_detail_opened"
private const val EVENT_SECTION_OPENED = "app_detail_section_opened"
private const val EVENT_ACTION_PERFORMED = "app_detail_action_performed"
private const val PARAMETER_ANALYSIS_MODE = "analysis_mode"
private const val PARAMETER_SECTION = "section"
private const val PARAMETER_ACTION = "action"

private val AppReference.analyticsAnalysisMode: String
    get() = when (this) {
        is AppReference.ApkFile -> "apk_file"
        is AppReference.InstalledPackage -> "installed_package"
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
    }
