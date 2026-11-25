package sk.styk.martin.apkanalyzer.feature.appdetail.phone.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput

@Serializable
internal data class NativeLibrariesNavKey(val detailInput: AppDetailInput, val appName: String) : NavKey
