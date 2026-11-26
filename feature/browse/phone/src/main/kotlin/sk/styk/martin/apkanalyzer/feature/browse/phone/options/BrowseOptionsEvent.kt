package sk.styk.martin.apkanalyzer.feature.browse.phone.options

import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseSubAttribute

internal sealed interface BrowseOptionsEvent {
    data class NavigateToApps(
        val bucketKey: String,
        val bucketLabel: String,
        val subAttribute: BrowseSubAttribute?,
    ) : BrowseOptionsEvent
}
