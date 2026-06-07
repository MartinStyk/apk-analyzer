package sk.styk.martin.apkanalyzer.feature.apps.impl.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import sk.styk.martin.apkanalyzer.core.common.util.FileUtil
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.core.uilibrary.animation.bottomEntryMetadata
import sk.styk.martin.apkanalyzer.core.uilibrary.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailNavKey
import sk.styk.martin.apkanalyzer.feature.apps.api.AppsNavKey
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.FilterScreen
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.permission.PermissionFilterScreen
import sk.styk.martin.apkanalyzer.feature.apps.impl.list.AppsScreen
import sk.styk.martin.apkanalyzer.feature.apps.impl.search.AppSearchScreen
import sk.styk.martin.apkanalyzer.feature.settings.api.SettingsNavKey

fun EntryProviderScope<NavKey>.appEntries(navigator: Navigator) {
    entry<AppsNavKey> {
        val context = LocalContext.current
        val apkFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                val cachedFile = FileUtil.copyUriToCache(context, uri)
                if (cachedFile != null) {
                    navigator.navigate(AppDetailNavKey(AppDetailInput.ApkFile(cachedFile.absolutePath)))
                }
            }
        }

        AppsScreen(
            onAppDetails = { packageName ->
                navigator.navigate(AppDetailNavKey(AppDetailInput.InstalledPackage(packageName)))
            },
            onSearch = {
                navigator.navigate(AppSearchNavKey)
            },
            onSettings = {
                navigator.navigate(SettingsNavKey)
            },
            onApkDetails = {
                apkFileLauncher.launch(arrayOf("application/vnd.android.package-archive"))
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
            onAppClick = { packageName ->
                navigator.navigate(AppDetailNavKey(AppDetailInput.InstalledPackage(packageName)))
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
