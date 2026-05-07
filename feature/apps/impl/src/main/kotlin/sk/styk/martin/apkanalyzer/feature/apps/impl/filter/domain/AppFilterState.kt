package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

@Immutable
data class AppFilterState(
    val selectedSources: ImmutableSet<AppSource> = persistentSetOf(),
    val selectedSdkVersions: ImmutableSet<Int> = persistentSetOf(),
    val apkSizeRange: AppSizeRange? = null,
    val installTimeRange: DateRange? = null,
    val updateTimeRange: DateRange? = null,
    val unusedPeriod: UnusedAppsPeriod? = null,
) {
    val isActive: Boolean
        get() = selectedSources.isNotEmpty() ||
            selectedSdkVersions.isNotEmpty() ||
            apkSizeRange != null ||
            installTimeRange != null ||
            updateTimeRange != null ||
            unusedPeriod != null

    val isLargeFilterActive: Boolean get() = apkSizeRange?.min != null && apkSizeRange.min >= LARGE_APP_THRESHOLD
    val isSystemFilterActive: Boolean get() = selectedSources == SYSTEM_ONLY_SOURCES
    val isSideloadedFilterActive: Boolean get() = selectedSources == SIDELOADED_ONLY_SOURCES
    val isRecentInstallActive: Boolean get() = installTimeRange?.start != null && installTimeRange.start > Instant.now() - TWO_MONTHS
    val isRecentUpdateActive: Boolean get() = updateTimeRange?.start != null && updateTimeRange.start > Instant.now() - TWO_MONTHS
    val isUnusedFilterActive: Boolean get() = unusedPeriod != null

    companion object {
        val LARGE_APP_THRESHOLD = 100.megabytes
        val TWO_MONTHS: Duration = 60.days.toJavaDuration()
        private val SYSTEM_ONLY_SOURCES = persistentSetOf(AppSource.SystemPreinstalled)
        private val SIDELOADED_ONLY_SOURCES = persistentSetOf(AppSource.Unknown)
    }
}

@Immutable
data class AppSizeRange(val min: AppSize, val max: AppSize) {
    operator fun contains(size: AppSize): Boolean = size in min..max
}

@Immutable
data class DateRange(val start: Instant, val end: Instant)
