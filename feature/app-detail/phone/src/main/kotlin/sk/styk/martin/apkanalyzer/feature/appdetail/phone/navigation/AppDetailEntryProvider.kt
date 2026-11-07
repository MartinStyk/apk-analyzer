package sk.styk.martin.apkanalyzer.feature.appdetail.phone.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.common.util.openAppSystemPage
import sk.styk.martin.apkanalyzer.core.common.util.openGooglePlay
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailNavKey
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.AppDetailScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentScope
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentsScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.IntentFiltersScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.certificates.CertificatesScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.AppDetailModeFrame
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.generalinfo.GeneralInfoScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.AppHistoryChangeScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.AppHistoryTimelineScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.manifest.ManifestScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.nativelibraries.NativeLibrariesScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.permissions.PermissionsScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.requirements.RequirementsScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.splitapks.SplitApksScreen
import sk.styk.martin.apkanalyzer.feature.appexport.api.AppInventoryExportNavKey

fun EntryProviderScope<NavKey>.appDetailEntries(navigator: Navigator) {
    entry<AppDetailNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        val context = LocalContext.current
        AppDetailModeFrame(input = key.detailInput) {
            AppDetailScreen(
                appDetailInput = key.detailInput,
                onBack = navigator::goBack,
                onOpenPlayStore = { packageName -> context.openGooglePlay(packageName) },
                onOpenAppInfo = { packageName -> context.openAppSystemPage(packageName) },
                onNavigateToManifest = { navigator.navigate(ManifestNavKey(key.detailInput)) },
                onNavigateToGeneralDetails = { navigator.navigate(GeneralInfoNavKey(key.detailInput)) },
                onNavigateToPermissions = { permissionName -> navigator.navigate(PermissionsNavKey(key.detailInput, permissionName)) },
                onNavigateToComponents = { navigator.navigate(ComponentsNavKey(key.detailInput)) },
                onNavigateToActivities = { navigator.navigate(ComponentsNavKey(key.detailInput, ComponentScope.Activities)) },
                onNavigateToServices = { navigator.navigate(ComponentsNavKey(key.detailInput, ComponentScope.Services)) },
                onNavigateToReceivers = { navigator.navigate(ComponentsNavKey(key.detailInput, ComponentScope.Receivers)) },
                onNavigateToProviders = { navigator.navigate(ComponentsNavKey(key.detailInput, ComponentScope.Providers)) },
                onNavigateToCertificates = { navigator.navigate(CertificatesNavKey(key.detailInput)) },
                onNavigateToFeatures = { navigator.navigate(RequirementsNavKey(key.detailInput)) },
                onNavigateToInventoryExport = { packageName -> navigator.navigate(AppInventoryExportNavKey(packageName.value)) },
                onNavigateToHistory = { packageName, appName -> navigator.navigate(AppHistoryTimelineNavKey(packageName.value, appName)) },
            )
        }
    }

    entry<GeneralInfoNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        AppDetailModeFrame(input = key.detailInput) {
            GeneralInfoScreen(
                appDetailInput = key.detailInput,
                onBack = { navigator.goBack() },
                onNavigateToSplitApks = { navigator.navigate(SplitApksNavKey(key.detailInput)) },
                onNavigateToNativeLibraries = { navigator.navigate(NativeLibrariesNavKey(key.detailInput)) },
            )
        }
    }

    entry<SplitApksNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        AppDetailModeFrame(input = key.detailInput) {
            SplitApksScreen(
                appDetailInput = key.detailInput,
                onBack = { navigator.goBack() },
            )
        }
    }

    entry<NativeLibrariesNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        AppDetailModeFrame(input = key.detailInput) {
            NativeLibrariesScreen(
                appDetailInput = key.detailInput,
                onBack = { navigator.goBack() },
            )
        }
    }

    entry<PermissionsNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        AppDetailModeFrame(input = key.detailInput) {
            PermissionsScreen(
                appDetailInput = key.detailInput,
                focusedPermission = key.focusedPermission,
                onBack = { navigator.goBack() },
            )
        }
    }

    entry<ComponentsNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        AppDetailModeFrame(input = key.detailInput) {
            ComponentsScreen(
                appDetailInput = key.detailInput,
                initialScope = key.scope,
                initialFilters = key.filters,
                onBack = { navigator.goBack() },
                onNavigateToIntentFilters = { componentName, componentType ->
                    navigator.navigate(IntentFiltersNavKey(key.detailInput, componentName, componentType))
                },
            )
        }
    }

    entry<IntentFiltersNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        AppDetailModeFrame(input = key.detailInput) {
            IntentFiltersScreen(
                appDetailInput = key.detailInput,
                componentName = key.componentName,
                componentType = key.componentType,
                onBack = { navigator.goBack() },
            )
        }
    }

    entry<CertificatesNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        AppDetailModeFrame(input = key.detailInput) {
            CertificatesScreen(
                appDetailInput = key.detailInput,
                onBack = { navigator.goBack() },
            )
        }
    }

    entry<RequirementsNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        AppDetailModeFrame(input = key.detailInput) {
            RequirementsScreen(
                appDetailInput = key.detailInput,
                onBack = { navigator.goBack() },
            )
        }
    }

    entry<ManifestNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        AppDetailModeFrame(input = key.detailInput) {
            ManifestScreen(
                appDetailInput = key.detailInput,
                onBack = { navigator.goBack() },
            )
        }
    }

    entry<AppHistoryTimelineNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        AppHistoryTimelineScreen(
            packageName = key.packageName,
            appName = key.appName,
            onBack = { navigator.goBack() },
            onNavigateToChange = { snapshotId, appName -> navigator.navigate(AppHistoryChangeNavKey(key.packageName, snapshotId, appName)) },
            onNavigateToFullDetail = { input -> navigator.navigate(AppDetailNavKey(input)) },
        )
    }

    entry<AppHistoryChangeNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        AppHistoryChangeScreen(
            packageName = key.packageName,
            snapshotId = key.snapshotId,
            appName = key.appName,
            onBack = { navigator.goBack() },
            onNavigateToFullDetail = { input -> navigator.navigate(AppDetailNavKey(input)) },
        )
    }
}
