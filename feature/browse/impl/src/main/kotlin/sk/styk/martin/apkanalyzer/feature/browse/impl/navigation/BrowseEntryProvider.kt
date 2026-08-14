package sk.styk.martin.apkanalyzer.feature.browse.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.core.uilibrary.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailNavKey
import sk.styk.martin.apkanalyzer.feature.browse.api.BrowseNavKey
import sk.styk.martin.apkanalyzer.feature.browse.impl.BrowseScreen
import sk.styk.martin.apkanalyzer.feature.browse.impl.apps.BrowseAppsScreen
import sk.styk.martin.apkanalyzer.feature.browse.impl.options.BrowseOptionsScreen

fun EntryProviderScope<NavKey>.browseEntries(navigator: Navigator) {
    entry<BrowseNavKey> {
        BrowseScreen(
            onOpenDimension = { dimension ->
                navigator.navigate(BrowseOptionsNavKey(dimension))
            },
        )
    }

    entry<BrowseOptionsNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        BrowseOptionsScreen(
            dimension = key.dimension,
            onBack = { navigator.goBack() },
            onOpenOption = { bucket ->
                navigator.navigate(BrowseAppsNavKey(key.dimension, bucket))
            },
        )
    }

    entry<BrowseAppsNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        BrowseAppsScreen(
            dimension = key.dimension,
            bucket = key.bucket,
            onBack = { navigator.goBack() },
            onOpenApp = { packageName ->
                navigator.navigate(AppDetailNavKey(AppDetailInput.InstalledPackage(packageName.value)))
            },
        )
    }
}
