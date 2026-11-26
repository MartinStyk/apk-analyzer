package sk.styk.martin.apkanalyzer.feature.browse.phone.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseSubAttribute

@Serializable
internal data class BrowseAppsNavKey(
    val dimension: BrowseDimension,
    val bucketKey: String,
    val bucketLabel: String,
    val subAttribute: BrowseSubAttribute?,
) : NavKey
