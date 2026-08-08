package sk.styk.martin.apkanalyzer.core.apps.analysis

import sk.styk.martin.apkanalyzer.core.apps.model.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.model.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

interface ManifestParser {
    suspend fun manifest(reference: AppReference): Result<ParsedManifest>

    suspend fun componentIntentFilters(reference: AppReference): Result<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>>
}

data class ParsedManifest(val xml: String, val additionalInstalledSplits: Int)
