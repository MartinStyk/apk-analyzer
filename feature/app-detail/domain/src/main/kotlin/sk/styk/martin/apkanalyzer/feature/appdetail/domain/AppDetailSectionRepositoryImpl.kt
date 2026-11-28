package sk.styk.martin.apkanalyzer.feature.appdetail.domain

import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryRecordedOn
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySection
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshotId
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshotRead
import sk.styk.martin.apkanalyzer.core.apps.api.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.api.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.api.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.api.intentfilters.IntentFiltersRepository
import sk.styk.martin.apkanalyzer.core.apps.api.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibrariesRepository
import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeRepository
import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppDetailSectionRepositoryImpl @Inject constructor(
    private val appDetailRepository: AppDetailRepository,
    private val nativeLibrariesRepository: NativeLibrariesRepository,
    private val signingSchemeRepository: SigningSchemeRepository,
    private val intentFiltersRepository: IntentFiltersRepository,
    private val appHistoryRepository: AppHistoryRepository,
) : AppDetailSectionRepository {

    @Volatile
    private var lastSnapshot: Pair<AppDetailInput.Historical, AppHistorySnapshot>? = null

    override suspend fun detail(input: AppDetailInput): Result<AppDetail> = when (input) {
        is AppDetailInput.Historical -> snapshot(input).map { it.detail }
        is AppDetailInput.InstalledPackage, is AppDetailInput.ApkFile -> appDetailRepository.details(input.live())
    }

    override suspend fun recordedOn(input: AppDetailInput): Result<AppHistoryRecordedOn> = when (input) {
        is AppDetailInput.Historical -> snapshot(input).map { it.recordedOn }
        is AppDetailInput.InstalledPackage, is AppDetailInput.ApkFile -> Result.success(AppHistoryRecordedOn.ThisDevice)
    }

    override suspend fun nativeLibraries(input: AppDetailInput): Result<AppDetailSection<NativeLibraries>> = when (input) {
        is AppDetailInput.Historical -> snapshot(input).map { it.nativeLibraries.toSection() }

        is AppDetailInput.InstalledPackage, is AppDetailInput.ApkFile -> nativeLibrariesRepository.nativeLibraries(input.live()).map {
            AppDetailSection.Available(it)
        }
    }

    override suspend fun signingSchemes(input: AppDetailInput): Result<List<SigningSchemeVersion>?> = when (input) {
        is AppDetailInput.Historical -> snapshot(input).map { (it.signingSchemes as? AppHistorySection.Recorded)?.value }
        is AppDetailInput.InstalledPackage, is AppDetailInput.ApkFile -> signingSchemeRepository.signingSchemeVersions(input.live())
    }

    override suspend fun intentFilters(input: AppDetailInput): Result<AppDetailSection<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>>> =
        when (input) {
            is AppDetailInput.Historical -> Result.success(AppDetailSection.NotRecorded)

            is AppDetailInput.InstalledPackage, is AppDetailInput.ApkFile -> intentFiltersRepository.componentIntentFilters(input.live()).map {
                AppDetailSection.Available(it)
            }
        }

    private fun AppDetailInput.live(): AppReference = checkNotNull(liveReference)

    private suspend fun snapshot(input: AppDetailInput.Historical): Result<AppHistorySnapshot> {
        lastSnapshot?.takeIf { it.first == input }?.let { return Result.success(it.second) }
        return when (val read = appHistoryRepository.snapshot(AppHistorySnapshotId(input.snapshotId))) {
            is AppHistorySnapshotRead.Found -> Result.success(read.snapshot.also { lastSnapshot = input to it })
            AppHistorySnapshotRead.NotFound -> Result.failure(RecordedVersionException(SectionErrorReason.VersionNotFound))
            AppHistorySnapshotRead.Unavailable -> Result.failure(RecordedVersionException(SectionErrorReason.VersionReadFailed))
        }
    }
}

private fun <T> AppHistorySection<T>.toSection(): AppDetailSection<T> = when (this) {
    is AppHistorySection.Recorded -> AppDetailSection.Available(value)
    AppHistorySection.NotRecorded -> AppDetailSection.NotRecorded
}
