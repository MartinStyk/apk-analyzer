package sk.styk.martin.apkanalyzer.core.apphistory.storage.util

import kotlinx.serialization.json.Json
import sk.styk.martin.apkanalyzer.core.apphistory.capture.APP_HISTORY
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryCaptureKey
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.storage.CapturedSection
import sk.styk.martin.apkanalyzer.core.apphistory.storage.StoredAppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistorySnapshotWithSectionsView
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.ActivitySnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.BroadcastReceiverSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.ComponentIntentFilterEntrySnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.ContentProviderSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.FeatureSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.InstalledSplitApkSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.NativeLibraryFileSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.PermissionsSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.ServiceSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.SigningSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot.toDomain
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.util.decompressBlob
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

private val snapshotJson = Json { ignoreUnknownKeys = true }

internal class AppHistoryRecordDecoder @Inject constructor(private val sdkVersionResolver: SdkVersionResolver) {

    fun decode(rows: List<AppHistorySnapshotWithSectionsView>): List<StoredAppHistoryRecord> {
        val decodedByHash = mutableMapOf<String, Any?>()
        return rows.map { it.toStoredRecord(decodedByHash) }
    }

    private fun AppHistorySnapshotWithSectionsView.toStoredRecord(decodedByHash: MutableMap<String, Any?>): StoredAppHistoryRecord {
        val row = snapshot
        val installSourceChain = InstallSourceChain(
            installingPackage = row.installingPackage?.let(::PackageName),
            initiatingPackage = row.initiatingPackage?.let(::PackageName),
            originatingPackage = row.originatingPackage?.let(::PackageName),
        )
        val info = AppInfo(
            packageName = PackageName(row.packageName),
            applicationName = row.applicationName,
            processName = row.processName,
            versionName = row.versionName,
            versionCode = row.versionCode,
            isSystemApp = row.isSystemApp,
            isDebuggable = row.isDebuggable,
            allowsBackup = row.allowsBackup,
            usesCleartextTraffic = row.usesCleartextTraffic,
            uid = row.uid,
            sharedUserId = row.sharedUserId,
            description = row.description,
            source = resolveAppInstallSource(installSourceChain, row.isSystemApp),
            installLocation = InstallLocation.valueOf(row.installLocation),
            installSourceChain = installSourceChain,
            apkSize = row.apkSize.bytes,
            firstInstallTime = Instant.ofEpochMilli(row.firstInstallTime),
            lastUpdateTime = Instant.ofEpochMilli(row.lastUpdateTime),
            minSdkVersion = row.minSdkVersion,
            minSdkLabel = sdkVersionResolver.resolveVersion(row.minSdkVersion),
            targetSdkVersion = row.targetSdkVersion,
            targetSdkLabel = sdkVersionResolver.resolveVersion(row.targetSdkVersion),
            installedSplits = decodeRequired<List<InstalledSplitApkSnapshot>, _>(
                "installedSplits",
                row.installedSplitsHash,
                installedSplitsContent,
                decodedByHash,
            ) { splits -> splits.map { it.toDomain() } },
        )
        val detail = AppDetail(
            analysisMode = AppDetail.AnalysisMode.Historical,
            info = info,
            signing = decodeRequired<SigningSnapshot, _>("signing", row.signingHash, signingContent, decodedByHash) { it.toDomain() },
            activities = decodeRequired<List<ActivitySnapshot>, _>("activities", row.activitiesHash, activitiesContent, decodedByHash) { activities ->
                activities.map { it.toDomain() }
            },
            services = decodeRequired<List<ServiceSnapshot>, _>("services", row.servicesHash, servicesContent, decodedByHash) { services ->
                services.map { it.toDomain() }
            },
            contentProviders = decodeRequired<List<ContentProviderSnapshot>, _>("providers", row.providersHash, providersContent, decodedByHash) { providers ->
                providers.map { it.toDomain() }
            },
            receivers = decodeRequired<List<BroadcastReceiverSnapshot>, _>("receivers", row.receiversHash, receiversContent, decodedByHash) { receivers ->
                receivers.map { it.toDomain() }
            },
            permissions = decodeRequired<PermissionsSnapshot, _>("permissions", row.permissionsHash, permissionsContent, decodedByHash) { it.toDomain() },
            features = decodeRequired<List<FeatureSnapshot>, _>("features", row.featuresHash, featuresContent, decodedByHash) { features ->
                features.map { it.toDomain() }
            },
        )
        val record = AppHistoryRecord(
            packageName = info.packageName,
            capturedAt = Instant.ofEpochMilli(row.capturedAt),
            captureKey = AppHistoryCaptureKey(
                deviceId = row.deviceId,
                firstInstallTime = Instant.ofEpochMilli(row.firstInstallTime),
                lastUpdateTime = Instant.ofEpochMilli(row.lastUpdateTime),
            ),
            detail = detail,
            intentFilters = decodeOptional<List<ComponentIntentFilterEntrySnapshot>, _>(
                "intentFilters",
                row.packageName,
                row.intentFiltersHash,
                intentFiltersContent,
                decodedByHash,
            ) { entries -> entries.associate { entry -> entry.key.toDomain() to entry.filters.map { it.toDomain() } } },
            nativeLibraries = decodeOptional<List<NativeLibraryFileSnapshot>, _>(
                "nativeLibraries",
                row.packageName,
                row.nativeLibrariesHash,
                nativeLibrariesContent,
                decodedByHash,
            ) { files -> NativeLibraries(files.map { it.toDomain() }) },
            signingScheme = decodeOptional<List<String>?, _>(
                "signingScheme",
                row.packageName,
                row.signingSchemeHash,
                signingSchemeContent,
                decodedByHash,
            ) { schemes -> schemes?.map { SigningSchemeVersion.valueOf(it) } },
        )
        return StoredAppHistoryRecord(id = row.id, record = record)
    }

    private inline fun <reified T, R> decodeRequired(
        section: String,
        hash: String?,
        content: ByteArray?,
        decodedByHash: MutableMap<String, Any?>,
        transform: (T) -> R,
    ): R {
        checkNotNull(hash) { "$section was not captured" }
        checkNotNull(content) { "$section blob is missing" }
        return transform(decodeCached<T>(section, hash, content, decodedByHash))
    }

    private inline fun <reified T, R> decodeOptional(
        section: String,
        packageName: String,
        hash: String?,
        content: ByteArray?,
        decodedByHash: MutableMap<String, Any?>,
        transform: (T) -> R,
    ): CapturedSection<R> {
        if (hash == null) return CapturedSection.Unavailable
        if (content == null) {
            Logger.w(APP_HISTORY, "$section blob missing for $packageName, section left unavailable")
            return CapturedSection.Unavailable
        }
        return try {
            CapturedSection.Captured(transform(decodeCached<T>(section, hash, content, decodedByHash)))
        } catch (exception: IOException) {
            Logger.w(APP_HISTORY, exception, "$section blob decompression failed for $packageName, section left unavailable")
            CapturedSection.Unavailable
        } catch (exception: IllegalArgumentException) {
            Logger.w(APP_HISTORY, exception, "$section blob decode failed for $packageName, section left unavailable")
            CapturedSection.Unavailable
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> decodeCached(
        section: String,
        hash: String,
        content: ByteArray,
        decodedByHash: MutableMap<String, Any?>,
    ): T = decodedByHash.getOrPut("$section:$hash") { snapshotJson.decodeFromString<T>(content.decompressBlob()) } as T
}
