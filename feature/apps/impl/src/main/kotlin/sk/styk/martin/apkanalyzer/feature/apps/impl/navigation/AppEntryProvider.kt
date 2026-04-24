package sk.styk.martin.apkanalyzer.feature.apps.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.apps.api.AppsNavKey
import sk.styk.martin.apkanalyzer.feature.apps.impl.list.AppsScreen
import sk.styk.martin.apkanalyzer.feature.apps.impl.search.AppSearchScreen
import sk.styk.martin.apkanalyzer.feature.settings.api.SettingsNavKey

fun EntryProviderScope<NavKey>.appEntries(navigator: Navigator) {
    entry<AppsNavKey> {
        AppsScreen(
            onAppDetails = {
                // navigator
            },
            onSearch = {
                navigator.navigate(AppSearchNavKey)
            },
            onSettings = {
                navigator.navigate(SettingsNavKey)
            },
            onApkDetails = {
            },
        )
    }

    entry<AppSearchNavKey> {
        AppSearchScreen(
            onAppClick = {
                // navigator
            },
            onBack = {
                navigator.goBack()
            },
        )
    }
}
