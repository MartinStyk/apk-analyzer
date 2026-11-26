package sk.styk.martin.apkanalyzer.feature.browse.phone.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseDimension

@Serializable
internal data class BrowseOptionsNavKey(val dimension: BrowseDimension) : NavKey
