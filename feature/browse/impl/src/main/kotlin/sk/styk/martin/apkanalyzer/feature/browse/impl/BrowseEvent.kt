package sk.styk.martin.apkanalyzer.feature.browse.impl

import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

internal sealed interface BrowseEvent {
    data class NavigateToDimension(val dimension: BrowseDimension) : BrowseEvent
}
