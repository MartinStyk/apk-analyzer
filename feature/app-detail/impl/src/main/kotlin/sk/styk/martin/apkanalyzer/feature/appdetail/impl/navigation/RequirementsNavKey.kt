package sk.styk.martin.apkanalyzer.feature.appdetail.impl.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput

@Serializable
internal data class RequirementsNavKey(val detailInput: AppDetailInput) : NavKey
