package sk.styk.martin.apkanalyzer.feature.appexport.phone.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.appexport.api.AppInventoryExportNavKey
import sk.styk.martin.apkanalyzer.feature.appexport.api.InventoryExportNavKey
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportScope
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportScreen
import sk.styk.martin.apkanalyzer.feature.appexport.phone.apps.InventoryExportFilterScope
import sk.styk.martin.apkanalyzer.feature.apps.api.AppFilterNavKey

fun EntryProviderScope<NavKey>.inventoryExportEntries(navigator: Navigator) {
    entry<InventoryExportNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) {
        InventoryExportScreen(
            scope = InventoryExportScope.InstalledApps,
            onBack = {
                navigator.goBack()
            },
            onEditFilters = {
                navigator.navigate(AppFilterNavKey(InventoryExportFilterScope.id))
            },
        )
    }

    entry<AppInventoryExportNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        InventoryExportScreen(
            scope = InventoryExportScope.SingleApp(PackageName(key.packageName)),
            onBack = {
                navigator.goBack()
            },
            onEditFilters = {},
        )
    }
}
