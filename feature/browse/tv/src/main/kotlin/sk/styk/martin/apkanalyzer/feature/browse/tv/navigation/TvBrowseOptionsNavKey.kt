package sk.styk.martin.apkanalyzer.feature.browse.tv.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseDimension

@Serializable
internal data class TvBrowseOptionsNavKey(val dimension: BrowseDimension) : NavKey
