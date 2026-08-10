package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import sk.styk.martin.apkanalyzer.core.apps.AppClassificationThresholds
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.isSideloaded
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

data class AppFilterState(
    val selectedSources: ImmutableSet<AppSource> = persistentSetOf(),
    val selectedSdkVersions: ImmutableSet<Int> = persistentSetOf(),
    val apkSizeRange: AppSizeRange? = null,
    val totalSizeRange: AppSizeRange? = null,
    val installTimeRange: DateRange? = null,
    val updateTimeRange: DateRange? = null,
    val unusedPeriod: UnusedAppsPeriod? = null,
    val recentlyUsedDays: Int? = null,
    val selectedPermissions: ImmutableSet<String> = persistentSetOf(),
    val permissionMatchAll: Boolean = false,
) {
    val isActive: Boolean
        get() = selectedSources.isNotEmpty() ||
            selectedSdkVersions.isNotEmpty() ||
            apkSizeRange != null ||
            totalSizeRange != null ||
            installTimeRange != null ||
            updateTimeRange != null ||
            unusedPeriod != null ||
            recentlyUsedDays != null ||
            selectedPermissions.isNotEmpty()

    val isLargeTotalFilterActive: Boolean get() = totalSizeRange?.min != null && totalSizeRange.min >= AppClassificationThresholds.LARGE_SIZE
    val isSystemFilterActive: Boolean get() = AppSource.SystemPreinstalled in selectedSources
    val isGooglePlayFilterActive: Boolean get() = AppSource.GooglePlay in selectedSources
    val isSideloadedFilterActive: Boolean get() = selectedSources.any { it.isSideloaded }
    val isRecentInstallActive: Boolean get() = installTimeRange?.start != null &&
        installTimeRange.start > Instant.now() - AppClassificationThresholds.RECENT_PERIOD - 1.days.toJavaDuration()
    val isRecentUpdateActive: Boolean get() = updateTimeRange?.start != null &&
        updateTimeRange.start > Instant.now() - AppClassificationThresholds.RECENT_PERIOD - 1.days.toJavaDuration()
    val isUnusedFilterActive: Boolean get() = unusedPeriod != null
    val isRecentlyUsedActive: Boolean get() = recentlyUsedDays != null
    val isSensitivePermissionsFilterActive: Boolean get() = PermissionPreset.Sensitive.permissions.all { it in selectedPermissions }
}

@Immutable
data class AppSizeRange(val min: AppSize, val max: AppSize) {
    operator fun contains(size: AppSize): Boolean = size in min..max
}

@Immutable
data class DateRange(val start: Instant, val end: Instant)
