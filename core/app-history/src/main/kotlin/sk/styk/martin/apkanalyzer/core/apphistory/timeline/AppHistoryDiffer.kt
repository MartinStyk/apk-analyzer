package sk.styk.martin.apkanalyzer.core.apphistory.timeline

import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.storage.CapturedSection
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryDiff
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.SetChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apps.model.AppInfo
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
            installSource = valueChange(beforeInfo.installSourceChain, afterInfo.installSourceChain),
            isDebuggable = valueChange(beforeInfo.isDebuggable, afterInfo.isDebuggable),
            allowsBackup = valueChange(beforeInfo.allowsBackup, afterInfo.allowsBackup),
            usesCleartextTraffic = valueChange(beforeInfo.usesCleartextTraffic, afterInfo.usesCleartextTraffic),
            requestedPermissions = setChange(
                before = beforeDetail.permissions.used,
                after = afterDetail.permissions.used,
                key = { it.permissionData.name },
                comparable = { },
            ),
            definedPermissions = setChange(
                before = beforeDetail.permissions.defined,
                after = afterDetail.permissions.defined,
                key = { it.name },
                comparable = { it.details?.copy(description = null) },
            ),
            activities = setChange(
                before = beforeDetail.activities,
                after = afterDetail.activities,
                key = { it.name },
                comparable = { it.copy(label = null) },
            ),
            services = setChange(beforeDetail.services, afterDetail.services, key = { it.name }),
            receivers = setChange(beforeDetail.receivers, afterDetail.receivers, key = { it.name }),
            providers = setChange(beforeDetail.contentProviders, afterDetail.contentProviders, key = { it.name }),
            certificates = setChange(
                before = beforeDetail.signing.currentCertificates,
                after = afterDetail.signing.currentCertificates,
                key = { it.certificateHashSha256 },
                comparable = { },
            ),
            signingSchemes = capturedSetChange(before.signingScheme, after.signingScheme, items = { it }, key = { it }),
            nativeLibraries = capturedSetChange(
                before = before.nativeLibraries,
                after = after.nativeLibraries,
                items = { it.files },
                key = { Triple(it.name, it.abi, it.containingApkFileName) },
            ),
        )
    }
}

private inline fun <S, T, K> capturedSetChange(
    before: CapturedSection<S>,
    after: CapturedSection<S>,
    items: (S) -> List<T>?,
    noinline key: (T) -> K,
): SetChange<T> {
    val beforeItems = (before as? CapturedSection.Captured)?.value?.let(items) ?: return SetChange.Uncomparable
    val afterItems = (after as? CapturedSection.Captured)?.value?.let(items) ?: return SetChange.Uncomparable
    return setChange(beforeItems, afterItems, key)
}

private fun <T, K> setChange(
    before: List<T>,
    after: List<T>,
    key: (T) -> K,
    comparable: (T) -> Any? = { it },
): SetChange<T> {
    val beforeByKey = before.associateBy(key)
    val afterByKey = after.associateBy(key)
    val added = after.filterNot { key(it) in beforeByKey }
    val removed = before.filterNot { key(it) in afterByKey }
    val modified = after.mapNotNull { item ->
        beforeByKey[key(item)]?.takeIf { comparable(it) != comparable(item) }?.let { ValueChange(before = it, after = item) }
    }
    return if (added.isEmpty() && removed.isEmpty() && modified.isEmpty()) {
        SetChange.Unchanged
    } else {
        SetChange.Changed(added = added, removed = removed, modified = modified)
    }
}

private fun <T> valueChange(before: T, after: T): ValueChange<T>? = if (before == after) null else ValueChange(before, after)

internal val AppInfo.version: AppHistoryVersion
    get() = AppHistoryVersion(versionCode = versionCode, versionName = versionName)
