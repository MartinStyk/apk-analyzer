package sk.styk.martin.apkanalyzer.feature.appdetail.impl.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.util.openAppSystemPage
import sk.styk.martin.apkanalyzer.core.common.util.openGooglePlay
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.core.uilibrary.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailNavKey
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.AppDetailScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.generalinfo.GeneralInfoScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions.PermissionsScreen

private const val TAG = "AppDetailNavigation"

fun EntryProviderScope<NavKey>.appDetailEntries(navigator: Navigator) {
    entry<AppDetailNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        val context = LocalContext.current
        AppDetailScreen(
            appDetailInput = key.detailInput,
            onBack = { navigator.goBack() },
            onOpenPlayStore = { packageName -> context.openGooglePlay(packageName) },
            onOpenAppInfo = { packageName -> context.openAppSystemPage(packageName) },
            onExportApk = { Logger.d(TAG, "Export APK not yet implemented") },
            onSaveIcon = { Logger.d(TAG, "Save icon not yet implemented") },
            onNavigateToManifest = { Logger.d(TAG, "Navigate to manifest not yet implemented") },
            onNavigateToGeneralDetails = { navigator.navigate(GeneralInfoNavKey(key.detailInput)) },
            onNavigateToPermissions = { navigator.navigate(PermissionsNavKey(key.detailInput)) },
            onNavigateToActivities = { Logger.d(TAG, "Navigate to activities not yet implemented") },
            onNavigateToServices = { Logger.d(TAG, "Navigate to services not yet implemented") },
            onNavigateToReceivers = { Logger.d(TAG, "Navigate to receivers not yet implemented") },
            onNavigateToProviders = { Logger.d(TAG, "Navigate to providers not yet implemented") },
            onNavigateToCertificates = { Logger.d(TAG, "Navigate to certificates not yet implemented") },
            onNavigateToFeatures = { Logger.d(TAG, "Navigate to features not yet implemented") },
        )
    }

    entry<GeneralInfoNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        GeneralInfoScreen(
            appDetailInput = key.detailInput,
            onBack = { navigator.goBack() },
        )
    }

    entry<PermissionsNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        PermissionsScreen(
            appDetailInput = key.detailInput,
            onBack = { navigator.goBack() },
        )
    }
}
