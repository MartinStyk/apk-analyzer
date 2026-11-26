package sk.styk.martin.apkanalyzer.feature.browse.phone

import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseDimension

internal sealed interface BrowseEvent {
    data class NavigateToDimension(val dimension: BrowseDimension) : BrowseEvent
}
