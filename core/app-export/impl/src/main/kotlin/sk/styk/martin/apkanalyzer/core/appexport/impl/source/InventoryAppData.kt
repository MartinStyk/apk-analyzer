package sk.styk.martin.apkanalyzer.core.appexport.impl.source

import sk.styk.martin.apkanalyzer.core.apps.api.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.api.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.api.manifest.ParsedManifest
import sk.styk.martin.apkanalyzer.core.apps.api.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.api.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.api.signing.AppSigning
import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeVersion

internal class InventoryAppData(
    val app: InstalledApp,
    val signing: SourceResult<AppSigning>,
    val detail: SourceResult<AppDetail>,
    val nativeLibraries: SourceResult<NativeLibraries>,
    val signingSchemes: SourceResult<List<SigningSchemeVersion>>,
    val intentFilters: SourceResult<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>>,
    val manifest: SourceResult<ParsedManifest>,
) {
    val isComplete: Boolean
        get() = listOf(signing, detail, nativeLibraries, signingSchemes, intentFilters, manifest).none { it is SourceResult.Failed }
}
