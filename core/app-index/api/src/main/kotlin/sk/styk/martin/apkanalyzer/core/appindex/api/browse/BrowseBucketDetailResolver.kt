package sk.styk.martin.apkanalyzer.core.appindex.api.browse

import sk.styk.martin.apkanalyzer.core.appindex.api.model.AppAttributeIndex

interface BrowseBucketDetailResolver {
    fun resolve(
        index: AppAttributeIndex,
        dimension: BrowseDimension,
        bucketKey: String,
        subAttribute: BrowseSubAttribute?,
    ): BrowseBucketDetail?
}
