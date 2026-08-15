package sk.styk.martin.apkanalyzer.feature.browse.impl

import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsEvent
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsTracker
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension
import javax.inject.Inject

internal class BrowseAnalytics @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) {

    fun track(event: BrowseAnalyticsEvent) {
        analyticsTracker.track(
            when (event) {
                BrowseAnalyticsEvent.TabOpened -> AnalyticsEvent(EVENT_BROWSE_TAB_OPENED)
                is BrowseAnalyticsEvent.DimensionOpened -> AnalyticsEvent(
                    EVENT_BROWSE_DIMENSION_OPENED,
                    mapOf(PARAMETER_DIMENSION to event.dimension.analyticsValue),
                )
                is BrowseAnalyticsEvent.CategoryOpened -> AnalyticsEvent(
                    EVENT_BROWSE_CATEGORY_OPENED,
                    mapOf(PARAMETER_DIMENSION to event.dimension.analyticsValue),
                )
            },
        )
    }
}

internal sealed interface BrowseAnalyticsEvent {
    data object TabOpened : BrowseAnalyticsEvent

    data class DimensionOpened(val dimension: BrowseDimension) : BrowseAnalyticsEvent

    data class CategoryOpened(val dimension: BrowseDimension) : BrowseAnalyticsEvent
}

private const val EVENT_BROWSE_TAB_OPENED = "browse_tab_opened"
private const val EVENT_BROWSE_DIMENSION_OPENED = "browse_dimension_opened"
private const val EVENT_BROWSE_CATEGORY_OPENED = "browse_category_opened"
private const val PARAMETER_DIMENSION = "dimension"

private val BrowseDimension.analyticsValue: String
    get() = when (this) {
        BrowseDimension.Permission -> "permission"
        BrowseDimension.SigningCertificate -> "signing_certificate"
        BrowseDimension.TargetSdk -> "target_sdk"
        BrowseDimension.MinSdk -> "min_sdk"
        BrowseDimension.InstallSource -> "install_source"
        BrowseDimension.SharedUserId -> "shared_user_id"
        BrowseDimension.AppCategory -> "app_category"
    }
