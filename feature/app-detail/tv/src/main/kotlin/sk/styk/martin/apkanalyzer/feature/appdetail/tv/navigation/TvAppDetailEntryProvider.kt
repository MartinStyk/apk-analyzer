package sk.styk.martin.apkanalyzer.feature.appdetail.tv.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailNavKey
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.TvAppDetailScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.appcomponents.TvComponentsScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.certificates.TvCertificatesScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.generalinfo.TvGeneralInfoScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.manifest.TvManifestScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.nativelibraries.TvNativeLibrariesScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.permissions.TvPermissionsScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.requirements.TvRequirementsScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.splitapks.TvSplitApksScreen

fun EntryProviderScope<NavKey>.tvAppDetailEntries(navigator: Navigator) {
    entry<AppDetailNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvAppDetailScreen(
            appDetailInput = key.detailInput,
            onNavigateToGeneralInfo = { appName -> navigator.navigate(TvGeneralInfoNavKey(key.detailInput, appName)) },
            onNavigateToPermissions = { appName, permissionName ->
                navigator.navigate(TvPermissionsNavKey(key.detailInput, appName, permissionName))
            },
            onNavigateToComponents = { appName -> navigator.navigate(TvComponentsNavKey(key.detailInput, appName)) },
            onNavigateToCertificates = { appName -> navigator.navigate(TvCertificatesNavKey(key.detailInput, appName)) },
            onNavigateToRequirements = { appName -> navigator.navigate(TvRequirementsNavKey(key.detailInput, appName)) },
            onNavigateToManifest = { appName -> navigator.navigate(TvManifestNavKey(key.detailInput, appName)) },
        )
    }

    entry<TvGeneralInfoNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvGeneralInfoScreen(
            appDetailInput = key.detailInput,
            appName = key.appName,
            onNavigateToSplitApks = { navigator.navigate(TvSplitApksNavKey(key.detailInput, key.appName)) },
            onNavigateToNativeLibraries = { navigator.navigate(TvNativeLibrariesNavKey(key.detailInput, key.appName)) },
        )
    }

    entry<TvPermissionsNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvPermissionsScreen(
            appDetailInput = key.detailInput,
            appName = key.appName,
            initialPermissionName = key.permissionName,
        )
    }

    entry<TvComponentsNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvComponentsScreen(appDetailInput = key.detailInput, appName = key.appName)
    }

    entry<TvCertificatesNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvCertificatesScreen(appDetailInput = key.detailInput, appName = key.appName)
    }

    entry<TvRequirementsNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvRequirementsScreen(appDetailInput = key.detailInput, appName = key.appName)
    }

    entry<TvNativeLibrariesNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvNativeLibrariesScreen(appDetailInput = key.detailInput, appName = key.appName)
    }

    entry<TvSplitApksNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvSplitApksScreen(appDetailInput = key.detailInput, appName = key.appName)
    }

    entry<TvManifestNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvManifestScreen(appDetailInput = key.detailInput, appName = key.appName)
    }
}
