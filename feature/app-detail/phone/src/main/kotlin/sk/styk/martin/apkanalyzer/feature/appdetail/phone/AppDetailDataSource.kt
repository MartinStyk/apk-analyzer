package sk.styk.martin.apkanalyzer.feature.appdetail.phone

import sk.styk.martin.apkanalyzer.core.apphistory.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySection
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySnapshotId
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySnapshotRead
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibrariesRepository
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeRepository
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import javax.inject.Inject

internal class RecordedVersionNotFoundException : Exception("This version is no longer in the app's history")

internal class RecordedVersionUnavailableException : Exception("This version could not be read from the app's history")

internal enum class RecordedVersionFailure {
    NotFound,
    ReadFailed,
}

internal fun Throwable.toRecordedVersionFailure(): RecordedVersionFailure? = when (this) {
    is RecordedVersionNotFoundException -> RecordedVersionFailure.NotFound
    is RecordedVersionUnavailableException -> RecordedVersionFailure.ReadFailed
    else -> null
}

internal class AppDetailDataSource @Inject constructor(
    private val appDetailRepository: AppDetailRepository,
    private val nativeLibrariesRepository: NativeLibrariesRepository,
    private val signingSchemeRepository: SigningSchemeRepository,
    private val appHistoryRepository: AppHistoryRepository,
) {
    private var recordedSnapshot: AppHistorySnapshot? = null

    suspend fun detail(input: AppDetailInput): Result<AppDetail> = when (input) {
        is AppDetailInput.Historical -> snapshot(input).map { it.detail }
        is AppDetailInput.InstalledPackage -> appDetailRepository.details(AppReference.InstalledPackage(PackageName(input.packageName)))
        is AppDetailInput.ApkFile -> appDetailRepository.details(AppReference.ApkFile(input.apkFilePath))
    }

    suspend fun nativeLibraries(input: AppDetailInput): Result<AppHistorySection<NativeLibraries>> = when (input) {
        is AppDetailInput.Historical -> snapshot(input).map { it.nativeLibraries }

        is AppDetailInput.InstalledPackage ->
            nativeLibrariesRepository.nativeLibraries(AppReference.InstalledPackage(PackageName(input.packageName))).map { AppHistorySection.Recorded(it) }

        is AppDetailInput.ApkFile ->
            nativeLibrariesRepository.nativeLibraries(AppReference.ApkFile(input.apkFilePath)).map { AppHistorySection.Recorded(it) }
    }

    suspend fun signingSchemes(input: AppDetailInput): Result<List<SigningSchemeVersion>?> = when (input) {
        is AppDetailInput.Historical -> snapshot(input).map { (it.signingSchemes as? AppHistorySection.Recorded)?.value }
        is AppDetailInput.InstalledPackage -> signingSchemeRepository.signingSchemeVersions(AppReference.InstalledPackage(PackageName(input.packageName)))
        is AppDetailInput.ApkFile -> signingSchemeRepository.signingSchemeVersions(AppReference.ApkFile(input.apkFilePath))
    }

    private suspend fun snapshot(input: AppDetailInput.Historical): Result<AppHistorySnapshot> {
        recordedSnapshot?.let { return Result.success(it) }
        return when (val read = appHistoryRepository.snapshot(PackageName(input.packageName), AppHistorySnapshotId(input.snapshotId))) {
            is AppHistorySnapshotRead.Found -> Result.success(read.snapshot.also { recordedSnapshot = it })
            AppHistorySnapshotRead.NotFound -> Result.failure(RecordedVersionNotFoundException())
            AppHistorySnapshotRead.Unavailable -> Result.failure(RecordedVersionUnavailableException())
        }
    }
}
