package sk.styk.martin.apkanalyzer.feature.browse.impl.options

import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.BrowseSubAttribute

internal sealed interface BrowseOptionsAction {
    data class ChangeQuery(val query: String) : BrowseOptionsAction
    data class SelectSubAttribute(val subAttribute: BrowseSubAttribute) : BrowseOptionsAction
    data class SelectOption(val option: BrowseOption) : BrowseOptionsAction
}
