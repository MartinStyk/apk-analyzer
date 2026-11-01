package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailNavKey
import sk.styk.martin.apkanalyzer.feature.apps.api.AppFilterNavKey
import sk.styk.martin.apkanalyzer.feature.apps.api.AppFilterScope
import sk.styk.martin.apkanalyzer.feature.inventoryexport.api.InventoryExportNavKey
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.apps.AppsStepScreen
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.columns.ColumnsStepScreen
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.InventoryExportStep
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.export.ExportStepScreen

fun EntryProviderScope<NavKey>.inventoryExportEntries(navigator: Navigator) {
    entry<InventoryExportNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) {
        AppsStepScreen(
            onBack = {
                navigator.goBack()
            },
            onNext = {
                navigator.navigate(ColumnsStepNavKey)
            },
            onEditFilters = {
                navigator.navigate(AppFilterNavKey(AppFilterScope.InventoryExport))
            },
            onAppDetails = { packageName ->
                navigator.navigate(AppDetailNavKey(AppDetailInput.InstalledPackage(packageName.value)))
            },
        )
    }

    entry<ColumnsStepNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) {
        ColumnsStepScreen(
            onBack = {
                navigator.goBack()
            },
            onNext = {
                navigator.navigate(ExportStepNavKey)
            },
            onSelectStep = { step ->
                navigator.returnToStep(from = InventoryExportStep.Columns, to = step)
            },
        )
    }

    entry<ExportStepNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) {
        ExportStepScreen(
            onBack = {
                navigator.goBack()
            },
            onSelectStep = { step ->
                navigator.returnToStep(from = InventoryExportStep.Export, to = step)
            },
        )
    }
}

private fun Navigator.returnToStep(from: InventoryExportStep, to: InventoryExportStep) {
    repeat(from.ordinal - to.ordinal) { goBack() }
}
