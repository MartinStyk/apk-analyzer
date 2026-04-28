package sk.styk.martin.apkanalyzer.feature.apps.impl.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.applist.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterRepository
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterState
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppSizeRange
import javax.inject.Inject

@HiltViewModel
class FilterViewModel @Inject constructor(private val appFilterRepository: AppFilterRepository, installedAppsRepository: InstalledAppsRepository) : ViewModel() {

    private val localFilter = MutableStateFlow(appFilterRepository.filter.value)
    private val showUnsavedChangesSheet = MutableStateFlow(false)

    private val eventChannel = Channel<FilterEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val appsMetadata = installedAppsRepository.apps()
        .map { apps ->
            val sdkVersions = apps.map { it.targetSdk }.distinct().sortedDescending().toPersistentList()
            val sizeRange = apps.minOfOrNull { it.apkSize }?.let { min ->
                apps.maxOfOrNull { it.apkSize }?.let { max ->
                    AppSizeRange(min, max)
                }
            }
            Pair(sdkVersions, sizeRange)
        }

    val state = combine(localFilter, appsMetadata, showUnsavedChangesSheet, appFilterRepository.filter) { filter, (sdkVersions, sizeRange), showSheet, savedFilter ->
        val effectiveFilter = if (sizeRange != null && filter.apkSizeRange != null && filter.apkSizeRange.max > sizeRange.max) {
            filter.copy(apkSizeRange = filter.apkSizeRange.copy(max = sizeRange.max))
        } else {
            filter
        }
        FilterState(
            filter = effectiveFilter,
            sizeFullRange = sizeRange,
            availableSdkVersions = sdkVersions,
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
                val isFullRange = state.value.sizeFullRange == current.apkSizeRange
                current.copy(apkSizeRange = if (isFullRange) null else action.range)
            }

            is FilterAction.InstallTimeRangeChanged -> localFilter.update { current ->
                current.copy(installTimeRange = action.range)
            }

            is FilterAction.InstallTimeRangeCleared -> localFilter.update { current ->
                current.copy(installTimeRange = null)
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
}
