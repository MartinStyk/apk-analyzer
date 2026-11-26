package sk.styk.martin.apkanalyzer.feature.browse.tv

import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseSubAttribute

internal sealed interface TvBrowseEvent {
    data class NavigateToDimension(val dimension: BrowseDimension) : TvBrowseEvent
    data class NavigateToApps(
        val dimension: BrowseDimension,
        val bucketKey: String,
        val bucketLabel: String,
        val subAttribute: BrowseSubAttribute?,
    ) : TvBrowseEvent
}
