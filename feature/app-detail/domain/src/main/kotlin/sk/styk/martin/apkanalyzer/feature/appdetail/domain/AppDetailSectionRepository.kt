package sk.styk.martin.apkanalyzer.feature.appdetail.domain

import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryRecordedOn
import sk.styk.martin.apkanalyzer.core.apps.api.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.api.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.api.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput

interface AppDetailSectionRepository {
    suspend fun detail(input: AppDetailInput): Result<AppDetail>

    suspend fun recordedOn(input: AppDetailInput): Result<AppHistoryRecordedOn>

    suspend fun nativeLibraries(input: AppDetailInput): Result<AppDetailSection<NativeLibraries>>

    suspend fun signingSchemes(input: AppDetailInput): Result<List<SigningSchemeVersion>?>

    suspend fun intentFilters(input: AppDetailInput): Result<AppDetailSection<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>>>
}

sealed interface AppDetailSection<out T> {
    data class Available<T>(val value: T) : AppDetailSection<T>

    data object NotRecorded : AppDetailSection<Nothing>
}

enum class SectionErrorReason {
    LoadFailed,
    VersionNotFound,
    VersionReadFailed,
}

class RecordedVersionException internal constructor(val reason: SectionErrorReason) : Exception("Recorded version read failed: $reason")

fun Throwable.toSectionErrorReason(): SectionErrorReason = (this as? RecordedVersionException)?.reason ?: SectionErrorReason.LoadFailed

suspend fun AppDetailSectionRepository.nativeLibrariesOrFallback(input: AppDetailInput): AppDetailSection<NativeLibraries> =
    nativeLibraries(input).getOrDefault(
        if (input.isHistorical) AppDetailSection.NotRecorded else AppDetailSection.Available(NativeLibraries.Empty),
    )
