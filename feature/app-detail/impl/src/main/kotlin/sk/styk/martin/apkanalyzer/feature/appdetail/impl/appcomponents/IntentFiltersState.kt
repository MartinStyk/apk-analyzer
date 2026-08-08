package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

internal sealed interface IntentFiltersState {
    data object Loading : IntentFiltersState

    data object Error : IntentFiltersState

    data object Unavailable : IntentFiltersState

    @Immutable
    data class Loaded(
        val componentName: String,
        val componentType: ComponentType,
        val query: String,
        val totalCount: Int,
        val filters: ImmutableList<ComponentIntentFilterItem>,
    ) : IntentFiltersState {
        val simpleComponentName: String
            get() = componentName.substringAfterLast('.')

        val hasResults: Boolean
            get() = filters.isNotEmpty()
    }
}
