package sk.styk.martin.apkanalyzer.feature.browse.impl.options

internal sealed interface BrowseOptionsEvent {
    data class NavigateToApps(
        val bucketKey: String,
        val bucketLabel: String,
        val subAttribute: String?,
    ) : BrowseOptionsEvent
}
