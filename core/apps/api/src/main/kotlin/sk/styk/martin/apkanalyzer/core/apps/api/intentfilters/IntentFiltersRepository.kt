package sk.styk.martin.apkanalyzer.core.apps.api.intentfilters

import sk.styk.martin.apkanalyzer.core.apps.api.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.api.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

interface IntentFiltersRepository {
    suspend fun componentIntentFilters(reference: AppReference): Result<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>>
}
