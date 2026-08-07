package sk.styk.martin.apkanalyzer.feature.browse.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.feature.browse.api.BrowseNavKey
import sk.styk.martin.apkanalyzer.feature.browse.impl.BrowseScreen

fun EntryProviderScope<NavKey>.browseEntries() {
    entry<BrowseNavKey> {
        BrowseScreen()
    }
}
