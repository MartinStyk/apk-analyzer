package sk.styk.martin.apkanalyzer.feature.appdetail.impl.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents.ComponentType

@Serializable
internal data class IntentFiltersNavKey(
    val detailInput: AppDetailInput,
    val componentName: String,
    val componentType: ComponentType,
) : NavKey
