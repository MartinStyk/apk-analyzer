package sk.styk.martin.apkanalyzer.feature.apps.impl.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.core.uilibrary.animation.bottomEntryMetadata
import sk.styk.martin.apkanalyzer.core.uilibrary.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.feature.apps.api.AppsNavKey
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.FilterScreen
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.permission.PermissionFilterScreen
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
            onFilter = {
                navigator.navigate(AppFilterNavKey)
            },
        )
    }

    entry<AppSearchNavKey>(
        metadata = NavDisplay.predictivePopTransitionSpec { _ ->
            EnterTransition.None togetherWith fadeOut(tween(200))
        },
    ) {
        AppSearchScreen(
            onAppClick = {
                // navigator
            },
            onBack = {
                navigator.goBack()
            },
            onFilter = {
                navigator.navigate(AppFilterNavKey)
            },
        )
    }

    entry<AppFilterNavKey>(
        metadata = bottomEntryMetadata(),
    ) {
        FilterScreen(
            onBack = {
                navigator.goBack()
            },
            onPermissionFilter = {
                navigator.navigate(PermissionFilterNavKey)
            },
        )
    }

    entry<PermissionFilterNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) {
        PermissionFilterScreen(
            onBack = {
                navigator.goBack()
            },
        )
    }
}
