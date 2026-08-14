package sk.styk.martin.apkanalyzer.feature.browse.impl.domain

import kotlinx.serialization.Serializable

@Serializable
internal data class BrowseBucketSelection(
    val key: String,
    val label: String,
    val subAttribute: BrowseSubAttribute?,
)
