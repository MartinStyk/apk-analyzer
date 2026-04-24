package sk.styk.martin.apkanalyzer.feature.settings.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.settings.api.SettingsNavKey
import sk.styk.martin.apkanalyzer.feature.settings.impl.SettingsScreen

fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
    entry<SettingsNavKey> {
        SettingsScreen(
            onBack = { navigator.goBack() },
        )
    }
}
