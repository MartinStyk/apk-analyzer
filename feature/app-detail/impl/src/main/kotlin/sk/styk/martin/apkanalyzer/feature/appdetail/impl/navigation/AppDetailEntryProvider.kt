package sk.styk.martin.apkanalyzer.feature.appdetail.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.core.uilibrary.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.feature.appdetail.api.ApkFileDetailNavKey
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailNavKey
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.AppDetailScreen

fun EntryProviderScope<NavKey>.appDetailEntries(navigator: Navigator) {
    entry<AppDetailNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) {
        AppDetailScreen(
            onBack = { navigator.goBack() },
        )
    }

    entry<ApkFileDetailNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) {
        AppDetailScreen(
            onBack = { navigator.goBack() },
        )
    }
}
