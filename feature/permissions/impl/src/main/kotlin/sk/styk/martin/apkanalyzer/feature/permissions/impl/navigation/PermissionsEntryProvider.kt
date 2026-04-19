package sk.styk.martin.apkanalyzer.feature.permissions.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.feature.permissions.api.PermissionsNavKey
import sk.styk.martin.apkanalyzer.feature.permissions.impl.PermissionsScreen

fun EntryProviderScope<NavKey>.permissionEntries() {
    entry<PermissionsNavKey> {
        PermissionsScreen()
    }
}
