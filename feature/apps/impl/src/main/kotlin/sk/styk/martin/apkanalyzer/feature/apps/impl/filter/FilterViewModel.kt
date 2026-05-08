package sk.styk.martin.apkanalyzer.feature.apps.impl.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.applist.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.applist.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.applist.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.combine
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterRepository
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterState
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppSizeRange
import javax.inject.Inject

@HiltViewModel
class FilterViewModel @Inject constructor(
    private val appFilterRepository: AppFilterRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val storageStatsRepository: StorageStatsRepository,
) : ViewModel() {

    private val localFilter = MutableStateFlow(appFilterRepository.filter.value)
    private val showUnsavedChangesSheet = MutableStateFlow(false)

    private val eventChannel = Channel<FilterEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val appsMetadata = installedAppsRepository.apps()
        .map { apps ->
            val sdkVersions = apps.distinctBy { it.targetSdk }.map { it.targetSdk }.sortedDescending()
            val apkSizeRange = apps.minOfOrNull { it.apkSize }?.let { min ->
                apps.maxOfOrNull { it.apkSize }?.let { max -> AppSizeRange(min, max) }
            }
            val totalSizes = apps.mapNotNull { it.totalSize }
            val totalSizeRange = totalSizes.minOrNull()?.let { min ->
                totalSizes.maxOrNull()?.let { max -> AppSizeRange(min, max) }
            }
            AppsMetadata(sdkVersions, apkSizeRange, totalSizeRange)
        }

    val state = combine(
        localFilter,
        appsMetadata,
        showUnsavedChangesSheet,
        appFilterRepository.filter,
        usageStatsRepository.isPermissionGranted,
        storageStatsRepository.isPermissionGranted,
    ) { filter, metadata, showSheet, savedFilter, usagePerm, storagePerm ->
        val effectiveFilter = if (metadata.apkSizeRange != null && filter.apkSizeRange != null &&
            filter.apkSizeRange.max > metadata.apkSizeRange.max
        ) {
            filter.copy(apkSizeRange = filter.apkSizeRange.copy(max = metadata.apkSizeRange.max))
        } else {
            filter
        }

        FilterState(
            filter = effectiveFilter,
            apkSizeSectionState = when {
                metadata.apkSizeRange == null -> ApkSizeSectionState.Loading
                else -> ApkSizeSectionState.RangeAvailable(metadata.apkSizeRange)
            },
            totalSizeSectionState = when {
                !storagePerm -> TotalSizeSectionState.PermissionMissing
                metadata.totalSizeRange == null -> TotalSizeSectionState.Loading
                else -> TotalSizeSectionState.RangeAvailable(metadata.totalSizeRange)
            },
            unusedAppsSectionState = when {
                !usagePerm -> UnusedAppsSectionState.PermissionMissing
                else -> UnusedAppsSectionState.Available
            },
            availableSdkVersions = metadata.sdkVersions.toImmutableList(),
            showUnsavedChangesSheet = showSheet,
            hasUnsavedChanges = effectiveFilter != savedFilter,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilterState())

    fun onAction(action: FilterAction) {
        when (action) {
            is FilterAction.SourceToggled -> localFilter.update { current ->
                val newSources = if (action.selected) {
                    (current.selectedSources + action.source).toPersistentSet()
                } else {
                    (current.selectedSources - action.source).toPersistentSet()
                }
                current.copy(selectedSources = newSources)
            }

            is FilterAction.SdkVersionToggled -> localFilter.update { current ->
                val newSdks = if (action.sdkVersion in current.selectedSdkVersions) {
                    (current.selectedSdkVersions - action.sdkVersion).toPersistentSet()
                } else {
                    (current.selectedSdkVersions + action.sdkVersion).toPersistentSet()
                }
                current.copy(selectedSdkVersions = newSdks)
            }

            is FilterAction.ApkSizeRangeChanged -> localFilter.update { current ->
                val bounds = (state.value.apkSizeSectionState as? ApkSizeSectionState.RangeAvailable)?.bounds
                current.copy(apkSizeRange = if (bounds == action.range) null else action.range)
            }

            is FilterAction.TotalSizeRangeChanged -> localFilter.update { current ->
                val bounds = (state.value.totalSizeSectionState as? TotalSizeSectionState.RangeAvailable)?.bounds
                current.copy(totalSizeRange = if (bounds == action.range) null else action.range)
            }

            is FilterAction.InstallTimeRangeChanged -> localFilter.update { current ->
                current.copy(installTimeRange = action.range)
            }

            is FilterAction.InstallTimeRangeCleared -> localFilter.update { current ->
                current.copy(installTimeRange = null)
            }

            is FilterAction.UnusedPeriodSelected -> localFilter.update { current ->
                current.copy(unusedPeriod = if (current.unusedPeriod == action.period) null else action.period)
            }

            FilterAction.OpenUsagePermissionSettings -> {
                eventChannel.trySend(FilterEvent.OpenUsagePermissionSettings)
            }

            FilterAction.Apply -> {
                appFilterRepository.update(localFilter.value)
                eventChannel.trySend(FilterEvent.NavigateBack)
            }

            FilterAction.Reset -> {
                localFilter.value = AppFilterState()
                appFilterRepository.update(localFilter.value)
            }

            FilterAction.NavigateBack -> {
                if (localFilter.value != appFilterRepository.filter.value) {
                    showUnsavedChangesSheet.value = true
                } else {
                    eventChannel.trySend(FilterEvent.NavigateBack)
                }
            }

            FilterAction.SaveAndClose -> {
                showUnsavedChangesSheet.value = false
                appFilterRepository.update(localFilter.value)
                eventChannel.trySend(FilterEvent.NavigateBack)
            }

            FilterAction.DiscardChanges -> {
                showUnsavedChangesSheet.value = false
                eventChannel.trySend(FilterEvent.NavigateBack)
            }

            FilterAction.DismissUnsavedChangesSheet -> {
                showUnsavedChangesSheet.value = false
            }
        }
    }

    private data class AppsMetadata(val sdkVersions: List<Int>, val apkSizeRange: AppSizeRange?, val totalSizeRange: AppSizeRange?)
}
