package sk.styk.martin.apkanalyzer.feature.appexport.phone

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

internal sealed interface InventoryExportScope {
    val steps: List<InventoryExportStep>

    data object InstalledApps : InventoryExportScope {
        override val steps = InventoryExportStep.entries
    }

    data class SingleApp(val packageName: PackageName) : InventoryExportScope {
        override val steps = listOf(InventoryExportStep.Columns, InventoryExportStep.Export)
    }
}
