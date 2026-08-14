package sk.styk.martin.apkanalyzer.feature.apps.impl.components.quickfilter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import sk.styk.martin.apkanalyzer.core.apps.storagestats.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.usagestats.UsageStatsRepository
import sk.styk.martin.apkanalyzer.feature.apps.impl.components.AppDataPermission
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterRepository
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.QuickFilter
import javax.inject.Inject

@HiltViewModel
class QuickFilterRowViewModel @Inject constructor(
    private val appFilterRepository: AppFilterRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val storageStatsRepository: StorageStatsRepository,
) : ViewModel() {

    private val eventChannel = Channel<QuickFilterRowEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val permissionRationale = MutableStateFlow<AppDataPermission?>(null)

    val state = combine(
        appFilterRepository.activeQuickFilters,
        appFilterRepository.activeSourceQuickFilters,
        appFilterRepository.activeActivityQuickFilter,
        appFilterRepository.filter,
        permissionRationale,
    ) { activeFilters, activeSources, activeActivity, filterState, rationale ->
        QuickFilterRowState(
            activeQuickFilters = activeFilters,
            activeSourceQuickFilters = activeSources,
            activeActivityQuickFilter = activeActivity,
            isDeepFilterActive = filterState.isActive,
            permissionRationale = rationale,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickFilterRowState())

    fun onAction(action: QuickFilterRowAction) {
        when (action) {
            is QuickFilterRowAction.QuickFilterToggle -> {
                val missingPermission = when {
                    action.filter == QuickFilter.Large && !storageStatsRepository.isPermissionGranted.value -> AppDataPermission.StorageAccess
                    else -> null
                }
                if (missingPermission != null) {
                    permissionRationale.value = missingPermission
                    return
                }
                appFilterRepository.toggleQuickFilter(action.filter)
            }

            is QuickFilterRowAction.SourceQuickFilterToggled -> appFilterRepository.toggleSourceQuickFilter(action.filter)

            is QuickFilterRowAction.ActivityQuickFilterSelected -> {
                if (action.filter != null && !usageStatsRepository.isPermissionGranted.value) {
                    permissionRationale.value = AppDataPermission.UsageAccess
                    return
                }
                appFilterRepository.selectActivityQuickFilter(action.filter)
            }

            is QuickFilterRowAction.FilterClick -> eventChannel.trySend(QuickFilterRowEvent.NavigateToFilter)

            is QuickFilterRowAction.DismissPermissionRationale -> permissionRationale.value = null

            is QuickFilterRowAction.OpenPermissionSettings -> {
                permissionRationale.value = null
                eventChannel.trySend(QuickFilterRowEvent.OpenUsageAccessSettings)
            }
        }
    }
}
