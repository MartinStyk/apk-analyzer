package sk.styk.martin.apkanalyzer.feature.browse.impl.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

@Serializable
internal data class BrowseOptionsNavKey(val dimension: BrowseDimension) : NavKey
