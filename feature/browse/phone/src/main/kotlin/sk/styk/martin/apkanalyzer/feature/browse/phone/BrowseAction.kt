package sk.styk.martin.apkanalyzer.feature.browse.phone

import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseDimension

internal sealed interface BrowseAction {
    data class SelectDimension(val dimension: BrowseDimension) : BrowseAction
}
