package sk.styk.martin.apkanalyzer.feature.appdetail.phone.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentFilter
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentScope

@Serializable
internal data class ComponentsNavKey(
    val detailInput: AppDetailInput,
    val appName: String,
    val scope: ComponentScope = ComponentScope.All,
    val filters: Set<ComponentFilter> = setOf(),
) : NavKey
