package sk.styk.martin.apkanalyzer.feature.apps.phone.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import sk.styk.martin.apkanalyzer.core.apps.filtering.AppFilterScope
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.animation.bottomEntryMetadata
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.appdetail.api.ApkFileLifetime
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailNavKey
import sk.styk.martin.apkanalyzer.feature.appexport.api.InventoryExportNavKey
import sk.styk.martin.apkanalyzer.feature.apps.api.AppFilterNavKey
import sk.styk.martin.apkanalyzer.feature.apps.api.AppsNavKey
import sk.styk.martin.apkanalyzer.feature.apps.phone.filter.FilterScreen
import sk.styk.martin.apkanalyzer.feature.apps.phone.filter.permission.PermissionFilterScreen
import sk.styk.martin.apkanalyzer.feature.apps.phone.filter.sdkversion.SdkVersionFilterScreen
import sk.styk.martin.apkanalyzer.feature.apps.phone.filter.source.SourceFilterScreen
import sk.styk.martin.apkanalyzer.feature.apps.phone.list.AppsScreen
import sk.styk.martin.apkanalyzer.feature.apps.phone.search.AppSearchScreen
import sk.styk.martin.apkanalyzer.feature.settings.api.SettingsNavKey

fun EntryProviderScope<NavKey>.appEntries(navigator: Navigator) {
    entry<AppsNavKey> {
        AppsScreen(
            onAppDetails = { packageName ->
                navigator.navigate(AppDetailNavKey(AppDetailInput.InstalledPackage(packageName.value)))
            },
            onSearch = {
                navigator.navigate(AppSearchNavKey)
            },
            onSettings = {
                navigator.navigate(SettingsNavKey)
            },
            onApkDetails = { apkFilePath ->
                navigator.navigate(
                    AppDetailNavKey(
                        AppDetailInput.ApkFile(
                            apkFilePath = apkFilePath,
                            lifetime = ApkFileLifetime.Temporary,
                        ),
                    ),
                )
            },
            onFilter = {
                navigator.navigate(AppFilterNavKey(AppFilterScope.List.id))
            },
            onInventoryExport = {
                navigator.navigate(InventoryExportNavKey)
            },
        )
    }

    entry<AppSearchNavKey>(
        metadata = NavDisplay.predictivePopTransitionSpec { _ ->
            EnterTransition.None togetherWith fadeOut(tween(200))
        },
    ) {
        AppSearchScreen(
            onAppClick = { packageName ->
                navigator.navigate(AppDetailNavKey(AppDetailInput.InstalledPackage(packageName.value)))
            },
            onBack = {
                navigator.goBack()
            },
            onFilter = {
                navigator.navigate(AppFilterNavKey(AppFilterScope.List.id))
            },
        )
    }

    entry<AppFilterNavKey>(
        metadata = bottomEntryMetadata(),
    ) { key ->
        FilterScreen(
            scopeId = key.scopeId,
            onBack = {
                navigator.goBack()
            },
            onPermissionFilter = {
                navigator.navigate(PermissionFilterNavKey)
            },
            onSourceFilter = {
                navigator.navigate(SourceFilterNavKey)
            },
            onSdkVersionFilter = {
                navigator.navigate(SdkVersionFilterNavKey)
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

    entry<SourceFilterNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) {
        SourceFilterScreen(
            onBack = {
                navigator.goBack()
            },
        )
    }

    entry<SdkVersionFilterNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) {
        SdkVersionFilterScreen(
            onBack = {
                navigator.goBack()
            },
        )
    }
}
