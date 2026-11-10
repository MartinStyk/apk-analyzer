package sk.styk.martin.apkanalyzer.feature.browse.phone.analytics

import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseDimension

internal sealed interface BrowseAnalyticsEvent {
    data object TabOpened : BrowseAnalyticsEvent

    data class DimensionOpened(val dimension: BrowseDimension) : BrowseAnalyticsEvent

    data class CategoryOpened(val dimension: BrowseDimension) : BrowseAnalyticsEvent
}
