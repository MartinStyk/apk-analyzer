package sk.styk.martin.apkanalyzer.core.apphistory.timeline

import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.storage.CapturedSection
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryDiff
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryFieldChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryInstallSource
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.NativeLibrary
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.SetChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.fieldChangesFrom
import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.Service
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.model.AppInfo
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryFile
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission
import javax.inject.Inject

internal class AppHistoryDiffer @Inject constructor() {

    operator fun invoke(before: AppHistoryRecord, after: AppHistoryRecord): AppHistoryDiff {
        val beforeDetail = before.detail
        val afterDetail = after.detail
        val beforeInfo = beforeDetail.info
        val afterInfo = afterDetail.info
        return AppHistoryDiff(
            version = valueChange(beforeInfo.version, afterInfo.version),
            minSdkVersion = valueChange(beforeInfo.minSdkVersion, afterInfo.minSdkVersion),
            targetSdkVersion = valueChange(beforeInfo.targetSdkVersion, afterInfo.targetSdkVersion),
            apkSize = valueChange(beforeInfo.apkSize, afterInfo.apkSize),
            installSource = valueChange(beforeInfo.installSource, afterInfo.installSource),
            isDebuggable = valueChange(beforeInfo.isDebuggable, afterInfo.isDebuggable),
            allowsBackup = valueChange(beforeInfo.allowsBackup, afterInfo.allowsBackup),
            usesCleartextTraffic = valueChange(beforeInfo.usesCleartextTraffic, afterInfo.usesCleartextTraffic),
            requestedPermissions = setChange(
                before = beforeDetail.permissions.used,
                after = afterDetail.permissions.used,
                key = { it.permissionData.name },
                isModified = { _, _ -> false },
            ),
            definedPermissions = setChange(
                before = beforeDetail.permissions.defined,
                after = afterDetail.permissions.defined,
                key = { it.name },
                isModified = hasVisibleChange(Permission::fieldChangesFrom),
            ),
            activities = setChange(beforeDetail.activities, afterDetail.activities, key = { it.name }, hasVisibleChange(Activity::fieldChangesFrom)),
            services = setChange(beforeDetail.services, afterDetail.services, key = { it.name }, hasVisibleChange(Service::fieldChangesFrom)),
            receivers = setChange(beforeDetail.receivers, afterDetail.receivers, key = { it.name }, hasVisibleChange(BroadcastReceiver::fieldChangesFrom)),
            providers = setChange(beforeDetail.contentProviders, afterDetail.contentProviders, key = {
                it.name
            }, hasVisibleChange(ContentProvider::fieldChangesFrom)),
            certificates = setChange(
                before = beforeDetail.signing.currentCertificates,
                after = afterDetail.signing.currentCertificates,
                key = { it.certificateHashSha256 },
                isModified = { _, _ -> false },
            ),
            signingSchemes = capturedSetChange(before.signingScheme, after.signingScheme, items = { it }, key = { it }),
            nativeLibraries = capturedSetChange(
                before = before.nativeLibraries,
                after = after.nativeLibraries,
                items = { it.files.groupedIntoLibraries() },
                key = { it.name },
                isModified = hasVisibleChange(NativeLibrary::fieldChangesFrom),
            ),
            requirements = setChange(
                before = beforeDetail.features,
                after = afterDetail.features,
                key = { it.requirementKey },
                isModified = hasVisibleChange(Feature::fieldChangesFrom),
            ),
        )
    }
}

private inline fun <S, T, K> capturedSetChange(
    before: CapturedSection<S>,
    after: CapturedSection<S>,
    items: (S) -> List<T>?,
    noinline key: (T) -> K,
    noinline isModified: (before: T, after: T) -> Boolean = { old, new -> old != new },
): SetChange<T> {
    val beforeItems = (before as? CapturedSection.Captured)?.value?.let(items) ?: return SetChange.Uncomparable
    val afterItems = (after as? CapturedSection.Captured)?.value?.let(items) ?: return SetChange.Uncomparable
    return setChange(beforeItems, afterItems, key, isModified)
}

private val Feature.requirementKey: String
    get() = when (this) {
        is Feature.Hardware -> name
        is Feature.OpenGlEs -> "opengles:$reqGlEsVersion"
    }

private fun List<NativeLibraryFile>.groupedIntoLibraries(): List<NativeLibrary> = groupBy { it.name }
    .map { (name, files) -> NativeLibrary(name, files.sortedWith(compareBy({ it.abi }, { it.containingApkFileName }))) }

private fun <T, K> setChange(
    before: List<T>,
    after: List<T>,
    key: (T) -> K,
    isModified: (before: T, after: T) -> Boolean = { old, new -> old != new },
): SetChange<T> {
    if (before === after) return SetChange.Unchanged
    val beforeByKey = before.associateBy(key)
    val afterByKey = after.associateBy(key)
    val added = after.filterNot { key(it) in beforeByKey }
    val removed = before.filterNot { key(it) in afterByKey }
    val modified = after.mapNotNull { item ->
        beforeByKey[key(item)]?.takeIf { isModified(it, item) }?.let { ValueChange(before = it, after = item) }
    }
    return if (added.isEmpty() && removed.isEmpty() && modified.isEmpty()) {
        SetChange.Unchanged
    } else {
        SetChange.Changed(added = added, removed = removed, modified = modified)
    }
}

private fun <T> hasVisibleChange(fieldChanges: T.(before: T) -> List<AppHistoryFieldChange>): (T, T) -> Boolean =
    { old, new -> old != new && new.fieldChanges(old).isNotEmpty() }

private fun <T> valueChange(before: T, after: T): ValueChange<T>? = if (before == after) null else ValueChange(before, after)

private val AppInfo.installSource: AppHistoryInstallSource
    get() = AppHistoryInstallSource(source = source, installingPackage = installSourceChain.installingPackage)

internal val AppInfo.version: AppHistoryVersion
    get() = AppHistoryVersion(versionCode = versionCode, versionName = versionName)
