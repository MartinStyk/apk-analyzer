package sk.styk.martin.apkanalyzer.feature.settings.phone.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.settings.api.SettingsNavKey
import sk.styk.martin.apkanalyzer.feature.settings.phone.BuildConfig
import sk.styk.martin.apkanalyzer.feature.settings.phone.SettingsScreen
import sk.styk.martin.apkanalyzer.feature.settings.phone.flags.FeatureFlagsScreen

fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
    entry<SettingsNavKey> {
        SettingsScreen(
            onBack = { navigator.goBack() },
            onFeatureFlags = { navigator.navigate(FeatureFlagsNavKey) },
        )
    }
    if (BuildConfig.DEBUG) {
        entry<FeatureFlagsNavKey>(metadata = slideFromEndEntryMetadata()) {
            FeatureFlagsScreen(onBack = { navigator.goBack() })
        }
    }
}
