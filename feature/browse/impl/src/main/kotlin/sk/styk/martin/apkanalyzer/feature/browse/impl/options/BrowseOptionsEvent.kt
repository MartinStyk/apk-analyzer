package sk.styk.martin.apkanalyzer.feature.browse.impl.options

import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.BrowseSubAttribute

internal sealed interface BrowseOptionsEvent {
    data class NavigateToApps(
        val bucketKey: String,
        val bucketLabel: String,
        val subAttribute: BrowseSubAttribute?,
    ) : BrowseOptionsEvent
}
