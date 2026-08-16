package sk.styk.martin.apkanalyzer.feature.browse.impl

import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsEvent
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsParameterName
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsTracker
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension
import javax.inject.Inject

internal class BrowseAnalytics @Inject constructor(private val analyticsTracker: AnalyticsTracker) {

    fun track(event: BrowseAnalyticsEvent) {
        analyticsTracker.track(
            when (event) {
                BrowseAnalyticsEvent.TabOpened -> AnalyticsEvent("browse_tab_opened")

                is BrowseAnalyticsEvent.DimensionOpened -> AnalyticsEvent(
                    "browse_dimension_opened",
                    mapOf(AnalyticsParameterName.Dimension to event.dimension.analyticsValue),
                )

                is BrowseAnalyticsEvent.CategoryOpened -> AnalyticsEvent(
                    "browse_category_opened",
                    mapOf(AnalyticsParameterName.Dimension to event.dimension.analyticsValue),
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
