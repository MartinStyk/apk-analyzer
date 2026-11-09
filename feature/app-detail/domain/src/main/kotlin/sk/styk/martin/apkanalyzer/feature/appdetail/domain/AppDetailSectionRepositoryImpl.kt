package sk.styk.martin.apkanalyzer.feature.appdetail.domain

import sk.styk.martin.apkanalyzer.core.apphistory.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySection
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySnapshotId
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySnapshotRead
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.intentfilters.IntentFiltersRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibrariesRepository
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeRepository
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
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
        else -> appDetailRepository.details(input.live())
    }

    override suspend fun nativeLibraries(input: AppDetailInput): Result<AppDetailSection<NativeLibraries>> = when (input) {
        is AppDetailInput.Historical -> snapshot(input).map { it.nativeLibraries.toSection() }
        else -> nativeLibrariesRepository.nativeLibraries(input.live()).map { AppDetailSection.Available(it) }
    }

    override suspend fun signingSchemes(input: AppDetailInput): Result<List<SigningSchemeVersion>?> = when (input) {
        is AppDetailInput.Historical -> snapshot(input).map { (it.signingSchemes as? AppHistorySection.Recorded)?.value }
        else -> signingSchemeRepository.signingSchemeVersions(input.live())
    }

    override suspend fun intentFilters(input: AppDetailInput): Result<AppDetailSection<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>>> =
        when (input) {
            is AppDetailInput.Historical -> Result.success(AppDetailSection.NotRecorded)
            else -> intentFiltersRepository.componentIntentFilters(input.live()).map { AppDetailSection.Available(it) }
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
