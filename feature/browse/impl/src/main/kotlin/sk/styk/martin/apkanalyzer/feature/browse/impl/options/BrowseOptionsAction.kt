package sk.styk.martin.apkanalyzer.feature.browse.impl.options

internal sealed interface BrowseOptionsAction {
    data class ChangeQuery(val query: String) : BrowseOptionsAction
    data class SelectSubAttribute(val key: String) : BrowseOptionsAction
    data class SelectOption(val option: BrowseOption) : BrowseOptionsAction
}
