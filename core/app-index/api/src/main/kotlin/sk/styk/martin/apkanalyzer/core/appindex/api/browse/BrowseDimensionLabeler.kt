package sk.styk.martin.apkanalyzer.core.appindex.api.browse

interface BrowseDimensionLabeler {
    fun label(
        dimension: BrowseDimension,
        key: String,
        subAttribute: BrowseSubAttribute? = null,
    ): String

    fun rawIdentifier(
        dimension: BrowseDimension,
        key: String,
        subAttribute: BrowseSubAttribute? = null,
    ): String?
}
