package sk.styk.martin.apkanalyzer.core.apphistory.storage.util

import sk.styk.martin.apkanalyzer.core.apphistory.capture.APP_HISTORY
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryCaptureKey
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.storage.CapturedSection
import sk.styk.martin.apkanalyzer.core.apphistory.storage.StoredAppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistoryBlobEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistorySnapshotEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.ActivitySnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.BroadcastReceiverSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.ContentProviderSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.FeatureSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.InstalledSplitApkSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.NativeLibraryFileSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.PermissionsSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.ServiceSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.SigningSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.toDomain
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.util.decompressBlob
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.util.snapshotJson
import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.apps.installsource.resolveAppInstallSource
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.AppInfo
import sk.styk.martin.apkanalyzer.core.apps.model.InstallLocation
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.sdkversion.SdkVersionResolver
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

internal class AppHistoryRecordDecoder @Inject constructor(private val sdkVersionResolver: SdkVersionResolver) {

    fun decode(rows: List<AppHistorySnapshotEntity>, blobs: List<AppHistoryBlobEntity>): List<StoredAppHistoryRecord> {
        val sections = SectionCache(blobs.associate { it.hash to it.content })
        return rows.mapNotNull { row -> row.toStoredRecordOrNull(sections) }
    }

    private fun AppHistorySnapshotEntity.toStoredRecordOrNull(sections: SectionCache): StoredAppHistoryRecord? = try {
        toStoredRecord(sections)
    } catch (exception: IllegalStateException) {
        Logger.w(APP_HISTORY, exception, "Snapshot $id of $packageName could not be decoded, left out of history")
        null
    } catch (exception: IllegalArgumentException) {
        Logger.w(APP_HISTORY, exception, "Snapshot $id of $packageName could not be decoded, left out of history")
        null
    } catch (exception: IOException) {
        Logger.w(APP_HISTORY, exception, "Snapshot $id of $packageName could not be decompressed, left out of history")
        null
    }

    private fun AppHistorySnapshotEntity.toStoredRecord(sections: SectionCache): StoredAppHistoryRecord {
        val installSourceChain = InstallSourceChain(
            installingPackage = installingPackage?.let(::PackageName),
            initiatingPackage = initiatingPackage?.let(::PackageName),
            originatingPackage = originatingPackage?.let(::PackageName),
        )
        val info = AppInfo(
            packageName = PackageName(packageName),
            applicationName = applicationName,
            processName = processName,
            versionName = versionName,
            versionCode = versionCode,
            isSystemApp = isSystemApp,
            isDebuggable = isDebuggable,
            allowsBackup = allowsBackup,
            usesCleartextTraffic = usesCleartextTraffic,
            uid = uid,
            sharedUserId = sharedUserId,
            description = description,
            source = resolveAppInstallSource(installSourceChain, isSystemApp),
            installLocation = InstallLocation.valueOf(installLocation),
            installSourceChain = installSourceChain,
            apkSize = apkSize.bytes,
            firstInstallTime = Instant.ofEpochMilli(firstInstallTime),
            lastUpdateTime = Instant.ofEpochMilli(lastUpdateTime),
            minSdkVersion = minSdkVersion,
            minSdkLabel = sdkVersionResolver.resolveVersion(minSdkVersion),
            targetSdkVersion = targetSdkVersion,
            targetSdkLabel = sdkVersionResolver.resolveVersion(targetSdkVersion),
            installedSplits = sections.required<List<InstalledSplitApkSnapshot>, _>("installedSplits", installedSplitsHash) { splits ->
                splits.map { it.toDomain() }
            },
        )
        val detail = AppDetail(
            analysisMode = AppDetail.AnalysisMode.Historical,
            info = info,
            signing = sections.required<SigningSnapshot, _>("signing", signingHash) { it.toDomain() },
            activities = sections.required<List<ActivitySnapshot>, _>("activities", activitiesHash) { activities ->
                activities.map { it.toDomain() }
            },
            services = sections.required<List<ServiceSnapshot>, _>("services", servicesHash) { services ->
                services.map { it.toDomain() }
            },
            contentProviders = sections.required<List<ContentProviderSnapshot>, _>("providers", providersHash) { providers ->
                providers.map { it.toDomain() }
            },
            receivers = sections.required<List<BroadcastReceiverSnapshot>, _>("receivers", receiversHash) { receivers ->
                receivers.map { it.toDomain() }
            },
            permissions = sections.required<PermissionsSnapshot, _>("permissions", permissionsHash) { it.toDomain() },
            features = sections.required<List<FeatureSnapshot>, _>("features", featuresHash) { features ->
                features.map { it.toDomain() }
            },
        )
        val record = AppHistoryRecord(
            packageName = info.packageName,
            capturedAt = Instant.ofEpochMilli(capturedAt),
            captureKey = AppHistoryCaptureKey(
                deviceId = deviceId,
                firstInstallTime = Instant.ofEpochMilli(firstInstallTime),
                lastUpdateTime = Instant.ofEpochMilli(lastUpdateTime),
                versionCode = versionCode,
            ),
            detail = detail,
            nativeLibraries = sections.optional<List<NativeLibraryFileSnapshot>, _>(
                "nativeLibraries",
                packageName,
                nativeLibrariesHash,
            ) { files -> NativeLibraries(files.map { it.toDomain() }) },
            signingScheme = sections.optional<List<String>?, _>(
                "signingScheme",
                packageName,
                signingSchemeHash,
            ) { schemes -> schemes?.map { SigningSchemeVersion.valueOf(it) } },
        )
        return StoredAppHistoryRecord(id = id, record = record)
    }
}

private class SectionCache(private val contentByHash: Map<Long, ByteArray>) {

    private val decoded = mutableMapOf<String, Any?>()

    inline fun <reified T, R> required(
        section: String,
        hash: Long?,
        transform: (T) -> R,
    ): R {
        checkNotNull(hash) { "$section was not captured" }
        val content = checkNotNull(contentByHash[hash]) { "$section blob is missing" }
        return decode(section, hash, content, transform)
    }

    inline fun <reified T, R> optional(
        section: String,
        packageName: String,
        hash: Long?,
        transform: (T) -> R,
    ): CapturedSection<R> {
        if (hash == null) return CapturedSection.Unavailable
        val content = contentByHash[hash]
        if (content == null) {
            Logger.w(APP_HISTORY, "$section blob missing for $packageName, section left unavailable")
            return CapturedSection.Unavailable
        }
        return try {
            CapturedSection.Captured(decode(section, hash, content, transform))
        } catch (exception: IOException) {
            Logger.w(APP_HISTORY, exception, "$section blob decompression failed for $packageName, section left unavailable")
            CapturedSection.Unavailable
        } catch (exception: IllegalArgumentException) {
            Logger.w(APP_HISTORY, exception, "$section blob decode failed for $packageName, section left unavailable")
            CapturedSection.Unavailable
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T, R> decode(
        section: String,
        hash: Long,
        content: ByteArray,
        transform: (T) -> R,
    ): R {
        val key = "$section:$hash"
        if (key in decoded) return decoded[key] as R
        return transform(snapshotJson.decodeFromString<T>(content.decompressBlob())).also { decoded[key] = it }
    }
}
