package sk.styk.martin.apkanalyzer.core.apps.analysis

import sk.styk.martin.apkanalyzer.core.common.model.AppReference

interface ManifestParser {
    suspend fun manifest(reference: AppReference): Result<ParsedManifest>
}

data class ParsedManifest(val xml: String, val additionalInstalledSplits: Int)
