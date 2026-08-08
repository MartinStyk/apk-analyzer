package sk.styk.martin.apkanalyzer.feature.browse.impl

import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

internal sealed interface BrowseAction {
    data class SelectDimension(val dimension: BrowseDimension) : BrowseAction
}
