package sk.styk.martin.apkanalyzer.feature.appdetail.tv.navigation

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.common.util.openAppSystemPage
import sk.styk.martin.apkanalyzer.core.common.util.openGooglePlay
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
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

fun EntryProviderScope<NavKey>.tvAppDetailEntries(navigator: Navigator) {
    entry<AppDetailNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        val context = LocalContext.current
        TvAppDetailScreen(
            appDetailInput = key.detailInput,
            onOpenAppInfo = { packageName ->
                context.openAppSystemPage(packageName).onFailure {
                    Toast.makeText(context, AppsR.string.app_info_unavailable, Toast.LENGTH_LONG).show()
                }
            },
            onOpenPlayStore = { packageName -> context.openGooglePlay(packageName) },
            onNavigateToGeneralInfo = { navigator.navigate(TvGeneralInfoNavKey(key.detailInput)) },
            onNavigateToPermissions = { permissionName -> navigator.navigate(TvPermissionsNavKey(key.detailInput, permissionName)) },
            onNavigateToComponents = { navigator.navigate(TvComponentsNavKey(key.detailInput)) },
            onNavigateToCertificates = { navigator.navigate(TvCertificatesNavKey(key.detailInput)) },
            onNavigateToRequirements = { navigator.navigate(TvRequirementsNavKey(key.detailInput)) },
            onNavigateToManifest = { navigator.navigate(TvManifestNavKey(key.detailInput)) },
        )
    }

    entry<TvGeneralInfoNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvGeneralInfoScreen(
            appDetailInput = key.detailInput,
            onNavigateToSplitApks = { navigator.navigate(TvSplitApksNavKey(key.detailInput)) },
            onNavigateToNativeLibraries = { navigator.navigate(TvNativeLibrariesNavKey(key.detailInput)) },
        )
    }

    entry<TvPermissionsNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvPermissionsScreen(appDetailInput = key.detailInput, initialPermissionName = key.permissionName)
    }

    entry<TvComponentsNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvComponentsScreen(appDetailInput = key.detailInput)
    }

    entry<TvCertificatesNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvCertificatesScreen(appDetailInput = key.detailInput)
    }

    entry<TvRequirementsNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvRequirementsScreen(appDetailInput = key.detailInput)
    }

    entry<TvNativeLibrariesNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvNativeLibrariesScreen(appDetailInput = key.detailInput)
    }

    entry<TvSplitApksNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvSplitApksScreen(appDetailInput = key.detailInput)
    }

    entry<TvManifestNavKey>(metadata = slideFromEndEntryMetadata()) { key ->
        TvManifestScreen(appDetailInput = key.detailInput)
    }
}
