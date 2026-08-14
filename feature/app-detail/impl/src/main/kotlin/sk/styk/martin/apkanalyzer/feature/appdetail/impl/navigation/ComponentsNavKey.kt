package sk.styk.martin.apkanalyzer.feature.appdetail.impl.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents.ComponentFilter
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents.ComponentScope

@Serializable
internal data class ComponentsNavKey(
    val detailInput: AppDetailInput,
    val scope: ComponentScope = ComponentScope.All,
    val filters: PersistentSet<ComponentFilter> = persistentSetOf(),
) : NavKey
