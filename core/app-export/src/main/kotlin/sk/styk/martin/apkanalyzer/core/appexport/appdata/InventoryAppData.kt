package sk.styk.martin.apkanalyzer.core.appexport.appdata

import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigning

internal class InventoryAppData(
    val app: InstalledApp,
    val signing: AppSigning?,
    val detail: AppDetail?,
    val isComplete: Boolean,
)
