package sk.styk.martin.apkanalyzer.feature.browse.impl.options

import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.BrowseBucketSelection

internal sealed interface BrowseOptionsEvent {
    data class NavigateToApps(val bucket: BrowseBucketSelection) : BrowseOptionsEvent
}
