package sk.styk.martin.apkanalyzer.core.appexport.source

import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.manifest.ParsedManifest
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigning
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion

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
