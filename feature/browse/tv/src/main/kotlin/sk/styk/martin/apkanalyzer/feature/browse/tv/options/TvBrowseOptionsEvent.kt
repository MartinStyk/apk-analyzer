package sk.styk.martin.apkanalyzer.feature.browse.tv.options

import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseSubAttribute

internal sealed interface TvBrowseOptionsEvent {
    data class NavigateToApps(
        val bucketKey: String,
        val bucketLabel: String,
        val subAttribute: BrowseSubAttribute?,
    ) : TvBrowseOptionsEvent
}
