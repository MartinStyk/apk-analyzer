package sk.styk.martin.apkanalyzer.core.apphistory.timeline.model

import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.Service
import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryFile
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.permissions.UsedPermission
import sk.styk.martin.apkanalyzer.core.apps.signing.Certificate
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.common.model.AppSize

data class AppHistoryDiff(
    val version: ValueChange<AppHistoryVersion>?,
    val minSdkVersion: ValueChange<Int?>?,
    val targetSdkVersion: ValueChange<Int?>?,
    val apkSize: ValueChange<AppSize>?,
    val installSource: ValueChange<InstallSourceChain>?,
    val isDebuggable: ValueChange<Boolean>?,
    val allowsBackup: ValueChange<Boolean>?,
    val usesCleartextTraffic: ValueChange<Boolean>?,
    val requestedPermissions: SetChange<UsedPermission>,
    val definedPermissions: SetChange<Permission>,
    val activities: SetChange<Activity>,
    val services: SetChange<Service>,
    val receivers: SetChange<BroadcastReceiver>,
    val providers: SetChange<ContentProvider>,
    val certificates: SetChange<Certificate>,
    val signingSchemes: SetChange<SigningSchemeVersion>,
    val nativeLibraries: SetChange<NativeLibraryFile>,
) {
    private val categoryStates: Map<AppHistoryChangeCategory, CategoryState>
        get() = mapOf(
            AppHistoryChangeCategory.Version to stateOf(values = listOf(version)),
            AppHistoryChangeCategory.SdkLevels to stateOf(values = listOf(minSdkVersion, targetSdkVersion)),
            AppHistoryChangeCategory.Size to stateOf(values = listOf(apkSize)),
            AppHistoryChangeCategory.InstallSource to stateOf(values = listOf(installSource)),
            AppHistoryChangeCategory.ManifestFlags to stateOf(values = listOf(isDebuggable, allowsBackup, usesCleartextTraffic)),
            AppHistoryChangeCategory.Permissions to stateOf(sets = listOf(requestedPermissions, definedPermissions)),
            AppHistoryChangeCategory.Components to stateOf(sets = listOf(activities, services, receivers, providers)),
            AppHistoryChangeCategory.Signing to stateOf(sets = listOf(certificates, signingSchemes)),
            AppHistoryChangeCategory.NativeLibraries to stateOf(sets = listOf(nativeLibraries)),
        ).filterValues { it != CategoryState.Unchanged }

    val changedCategories: Set<AppHistoryChangeCategory>
        get() = categoryStates.filterValues { it == CategoryState.Changed }.keys

    val uncomparableCategories: Set<AppHistoryChangeCategory>
        get() = categoryStates.filterValues { it == CategoryState.Uncomparable }.keys

    private fun stateOf(values: List<ValueChange<*>?> = emptyList(), sets: List<SetChange<*>> = emptyList()): CategoryState = when {
        values.any { it != null } || sets.any { it is SetChange.Changed } -> CategoryState.Changed
        sets.any { it is SetChange.Uncomparable } -> CategoryState.Uncomparable
        else -> CategoryState.Unchanged
    }

    private enum class CategoryState { Unchanged, Changed, Uncomparable }
}

enum class AppHistoryChangeCategory {
    Version,
    SdkLevels,
    Size,
    InstallSource,
    ManifestFlags,
    Permissions,
    Components,
    Signing,
    NativeLibraries,
}

data class ValueChange<out T>(val before: T, val after: T)

sealed interface SetChange<out T> {
    data object Unchanged : SetChange<Nothing>

    data object Uncomparable : SetChange<Nothing>

    data class Changed<T>(
        val added: List<T>,
        val removed: List<T>,
        val modified: List<ValueChange<T>>,
    ) : SetChange<T>
}
