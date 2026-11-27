package sk.styk.martin.apkanalyzer.feature.appdetail.phone.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailNavKey
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppHistoryChangeNavKey
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppHistoryTimelineNavKey
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.AppDetailScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentScope
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentsScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.IntentFiltersScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.certificates.CertificatesScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.generalinfo.GeneralInfoScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.change.AppHistoryChangeScreen
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.timeline.AppHistoryTimelineScreen
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
        AppDetailScreen(
            appDetailInput = key.detailInput,
            onBack = navigator::goBack,
            onNavigateToManifest = { appName -> navigator.navigate(ManifestNavKey(key.detailInput, appName)) },
            onNavigateToGeneralDetails = { appName -> navigator.navigate(GeneralInfoNavKey(key.detailInput, appName)) },
            onNavigateToPermissions = { appName, permissionName ->
                navigator.navigate(PermissionsNavKey(key.detailInput, appName, permissionName))
            },
            onNavigateToComponents = { appName -> navigator.navigate(ComponentsNavKey(key.detailInput, appName)) },
            onNavigateToActivities = { appName ->
                navigator.navigate(ComponentsNavKey(key.detailInput, appName, ComponentScope.Activities))
            },
            onNavigateToServices = { appName ->
                navigator.navigate(ComponentsNavKey(key.detailInput, appName, ComponentScope.Services))
            },
            onNavigateToReceivers = { appName ->
                navigator.navigate(ComponentsNavKey(key.detailInput, appName, ComponentScope.Receivers))
            },
            onNavigateToProviders = { appName ->
                navigator.navigate(ComponentsNavKey(key.detailInput, appName, ComponentScope.Providers))
            },
            onNavigateToCertificates = { appName -> navigator.navigate(CertificatesNavKey(key.detailInput, appName)) },
            onNavigateToFeatures = { appName -> navigator.navigate(RequirementsNavKey(key.detailInput, appName)) },
            onNavigateToInventoryExport = { packageName -> navigator.navigate(AppInventoryExportNavKey(packageName.value)) },
            onNavigateToHistory = { packageName, appName -> navigator.navigate(AppHistoryTimelineNavKey(packageName.value, appName)) },
        )
    }

    entry<GeneralInfoNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        GeneralInfoScreen(
            appDetailInput = key.detailInput,
            appName = key.appName,
            onBack = { navigator.goBack() },
            onNavigateToSplitApks = { navigator.navigate(SplitApksNavKey(key.detailInput, key.appName)) },
            onNavigateToNativeLibraries = { navigator.navigate(NativeLibrariesNavKey(key.detailInput, key.appName)) },
        )
    }

    entry<SplitApksNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        SplitApksScreen(
            appDetailInput = key.detailInput,
            appName = key.appName,
            onBack = { navigator.goBack() },
        )
    }

    entry<NativeLibrariesNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        NativeLibrariesScreen(
            appDetailInput = key.detailInput,
            appName = key.appName,
            onBack = { navigator.goBack() },
        )
    }

    entry<PermissionsNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        PermissionsScreen(
            appDetailInput = key.detailInput,
            appName = key.appName,
            focusedPermission = key.focusedPermission,
            onBack = { navigator.goBack() },
        )
    }

    entry<ComponentsNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        ComponentsScreen(
            appDetailInput = key.detailInput,
            appName = key.appName,
            initialScope = key.scope,
            initialFilters = key.filters,
            onBack = { navigator.goBack() },
            onNavigateToIntentFilters = { componentName, componentType ->
                navigator.navigate(IntentFiltersNavKey(key.detailInput, key.appName, componentName, componentType))
            },
        )
    }

    entry<IntentFiltersNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        IntentFiltersScreen(
            appDetailInput = key.detailInput,
            appName = key.appName,
            componentName = key.componentName,
            componentType = key.componentType,
            onBack = { navigator.goBack() },
        )
    }

    entry<CertificatesNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        CertificatesScreen(
            appDetailInput = key.detailInput,
            appName = key.appName,
            onBack = { navigator.goBack() },
        )
    }

    entry<RequirementsNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        RequirementsScreen(
            appDetailInput = key.detailInput,
            appName = key.appName,
            onBack = { navigator.goBack() },
        )
    }

    entry<ManifestNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        ManifestScreen(
            appDetailInput = key.detailInput,
            appName = key.appName,
            onBack = { navigator.goBack() },
        )
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
