package sk.styk.martin.apkanalyzer.feature.apps.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.feature.apps.api.AppsNavKey
import sk.styk.martin.apkanalyzer.feature.apps.impl.AppsScreen

fun EntryProviderScope<NavKey>.appEntries() {
    entry<AppsNavKey> {
        AppsScreen()
    }
}
