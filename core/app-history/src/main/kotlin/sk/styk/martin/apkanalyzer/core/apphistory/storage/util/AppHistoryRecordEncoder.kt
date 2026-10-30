package sk.styk.martin.apkanalyzer.core.apphistory.storage.util

import kotlinx.serialization.json.Json
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.storage.CapturedSection
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistoryBlobEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistorySnapshotEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.ComponentIntentFilterEntrySnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.toSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.util.compressBlob
import sk.styk.martin.apkanalyzer.core.common.digest.DigestManager
import javax.inject.Inject

private const val SECTION_HASH_HEX_LENGTH = 16

internal class EncodedAppHistoryRecord(val snapshot: AppHistorySnapshotEntity, val blobs: List<AppHistoryBlobEntity>)

internal class AppHistoryRecordEncoder @Inject constructor(private val digestManager: DigestManager) {

    fun encode(record: AppHistoryRecord): EncodedAppHistoryRecord {
        val sections = hashSections(record)
        return EncodedAppHistoryRecord(snapshot = record.toSnapshotEntity(sections), blobs = sections.blobs)
    }

    private fun hashSections(record: AppHistoryRecord): HashedSections {
        val packageName = record.packageName.value
        val detail = record.detail
        return HashedSections(
            permissions = hashSection(packageName, detail.permissions.toSnapshot()),
            activities = hashSection(packageName, detail.activities.map { it.toSnapshot() }.sortedBy { it.name }),
            services = hashSection(packageName, detail.services.map { it.toSnapshot() }.sortedBy { it.name }),
            receivers = hashSection(packageName, detail.receivers.map { it.toSnapshot() }.sortedBy { it.name }),
            providers = hashSection(packageName, detail.contentProviders.map { it.toSnapshot() }.sortedBy { it.name }),
            features = hashSection(packageName, detail.features.map { it.toSnapshot() }.sortedBy { Json.encodeToString(it) }),
            signing = hashSection(packageName, detail.signing.toSnapshot()),
            installedSplits = hashSection(packageName, detail.info.installedSplits.map { it.toSnapshot() }.sortedBy { it.fileName }),
            intentFilters = record.intentFilters.valueOrNull()?.let { filters ->
                val entries = filters.map { (key, value) ->
                    ComponentIntentFilterEntrySnapshot(key.toSnapshot(), value.map { it.toSnapshot() }.sortedBy { Json.encodeToString(it) })
                }.sortedWith(compareBy({ it.key.name }, { it.key.kind }))
                hashSection(packageName, entries)
            },
            nativeLibraries = record.nativeLibraries.valueOrNull()?.let { libraries ->
                val sorted = libraries.files.map { it.toSnapshot() }.sortedWith(compareBy({ it.name }, { it.abi }, { it.containingApkFileName }))
                hashSection(packageName, sorted)
            },
            signingScheme = (record.signingScheme as? CapturedSection.Captured)?.let { captured ->
                hashSection(packageName, captured.value?.map { it.name }?.sorted())
            },
        )
    }

    private inline fun <reified T> hashSection(packageName: String, content: T): HashedSection {
        val serialized = Json.encodeToString(content)
        val hash = digestManager.sha256Digest(serialized).take(SECTION_HASH_HEX_LENGTH).toULong(radix = 16).toLong()
        return HashedSection(hash, AppHistoryBlobEntity(packageName = packageName, hash = hash, content = serialized.compressBlob()))
    }

    private fun AppHistoryRecord.toSnapshotEntity(sections: HashedSections): AppHistorySnapshotEntity {
        val info = detail.info
        return AppHistorySnapshotEntity(
            packageName = packageName.value,
            deviceId = captureKey.deviceId,
            capturedAt = capturedAt.toEpochMilli(),
            firstInstallTime = captureKey.firstInstallTime.toEpochMilli(),
            lastUpdateTime = captureKey.lastUpdateTime.toEpochMilli(),
            applicationName = info.applicationName,
            processName = info.processName,
            versionCode = info.versionCode,
            versionName = info.versionName,
            isSystemApp = info.isSystemApp,
            isDebuggable = info.isDebuggable,
            allowsBackup = info.allowsBackup,
            usesCleartextTraffic = info.usesCleartextTraffic,
            uid = info.uid,
            sharedUserId = info.sharedUserId,
            description = info.description,
            installLocation = info.installLocation.name,
            installingPackage = info.installSourceChain.installingPackage?.value,
            initiatingPackage = info.installSourceChain.initiatingPackage?.value,
            originatingPackage = info.installSourceChain.originatingPackage?.value,
            apkSize = info.apkSize.bytes,
            targetSdkVersion = info.targetSdkVersion,
            minSdkVersion = info.minSdkVersion,
            permissionsHash = sections.permissions.hash,
            activitiesHash = sections.activities.hash,
            servicesHash = sections.services.hash,
            receiversHash = sections.receivers.hash,
            providersHash = sections.providers.hash,
            featuresHash = sections.features.hash,
            signingHash = sections.signing.hash,
            intentFiltersHash = sections.intentFilters?.hash,
            nativeLibrariesHash = sections.nativeLibraries?.hash,
            signingSchemeHash = sections.signingScheme?.hash,
            installedSplitsHash = sections.installedSplits.hash,
        )
    }

    private class HashedSection(val hash: Long, val blob: AppHistoryBlobEntity)

    private class HashedSections(
        val permissions: HashedSection,
        val activities: HashedSection,
        val services: HashedSection,
        val receivers: HashedSection,
        val providers: HashedSection,
        val features: HashedSection,
        val signing: HashedSection,
        val installedSplits: HashedSection,
        val intentFilters: HashedSection?,
        val nativeLibraries: HashedSection?,
        val signingScheme: HashedSection?,
    ) {
        val blobs: List<AppHistoryBlobEntity>
            get() = listOfNotNull(
                permissions.blob, activities.blob, services.blob, receivers.blob, providers.blob,
                features.blob, signing.blob, installedSplits.blob,
                intentFilters?.blob, nativeLibraries?.blob, signingScheme?.blob,
            )
    }
}

private fun <T> CapturedSection<T>.valueOrNull(): T? = (this as? CapturedSection.Captured)?.value
