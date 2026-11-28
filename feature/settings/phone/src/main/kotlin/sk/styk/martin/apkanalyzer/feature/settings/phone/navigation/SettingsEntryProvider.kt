package sk.styk.martin.apkanalyzer.feature.settings.phone.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.settings.api.AppHistorySettingsNavKey
import sk.styk.martin.apkanalyzer.feature.settings.api.SettingsNavKey
import sk.styk.martin.apkanalyzer.feature.settings.phone.BuildConfig
import sk.styk.martin.apkanalyzer.feature.settings.phone.SettingsScreen
import sk.styk.martin.apkanalyzer.feature.settings.phone.flags.FeatureFlagsScreen
import sk.styk.martin.apkanalyzer.feature.settings.phone.history.AppHistorySettingsScreen

fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
    entry<SettingsNavKey> {
        SettingsScreen(
            onBack = { navigator.goBack() },
            onAppHistorySettings = { navigator.navigate(AppHistorySettingsNavKey) },
            onFeatureFlags = { navigator.navigate(FeatureFlagsNavKey) },
        )
    }
    entry<AppHistorySettingsNavKey>(metadata = slideFromEndEntryMetadata()) {
        AppHistorySettingsScreen(onBack = { navigator.goBack() })
    }
    if (BuildConfig.DEBUG) {
        entry<FeatureFlagsNavKey>(metadata = slideFromEndEntryMetadata()) {
            FeatureFlagsScreen(onBack = { navigator.goBack() })
        }
    }
}
