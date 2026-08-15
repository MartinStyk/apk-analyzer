package sk.styk.martin.apkanalyzer.feature.browse.impl.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.BrowseSubAttribute
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

@Serializable
internal data class BrowseAppsNavKey(
    val dimension: BrowseDimension,
    val bucketKey: String,
    val bucketLabel: String,
    val subAttribute: BrowseSubAttribute?,
) : NavKey
