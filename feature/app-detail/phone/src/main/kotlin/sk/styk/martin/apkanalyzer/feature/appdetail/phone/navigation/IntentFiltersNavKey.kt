package sk.styk.martin.apkanalyzer.feature.appdetail.phone.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentType

@Serializable
internal data class IntentFiltersNavKey(
    val detailInput: AppDetailInput,
    val appName: String,
    val componentName: String,
    val componentType: ComponentType,
) : NavKey
