package sk.styk.martin.apkanalyzer.feature.appdetail.tv.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput

@Serializable
internal data class TvComponentsNavKey(val detailInput: AppDetailInput, val appName: String) : NavKey
