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
import sk.styk.martin.apkanalyzer.core.applist.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.applist.UsageStatsRepository
import sk.styk.martin.apkanalyzer.feature.apps.impl.components.AppDataPermission
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterRepository
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.QuickFilter
import javax.inject.Inject

@HiltViewModel
class QuickFilterRowViewModel @Inject constructor(private val appFilterRepository: AppFilterRepository, private val usageStatsRepository: UsageStatsRepository, private val storageStatsRepository: StorageStatsRepository) : ViewModel() {

    private val eventChannel = Channel<QuickFilterRowEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val permissionRationale = MutableStateFlow<AppDataPermission?>(null)

    val state = combine(
        appFilterRepository.activeQuickFilters,
        appFilterRepository.filter,
        permissionRationale,
    ) { activeFilters, filterState, rationale ->
        QuickFilterRowState(
            activeQuickFilters = activeFilters,
            isDeepFilterActive = filterState.isActive,
            permissionRationale = rationale,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickFilterRowState())

    fun onAction(action: QuickFilterRowAction) {
        when (action) {
            is QuickFilterRowAction.QuickFilterToggle -> {
                val missingPermission = when {
                    action.filter == QuickFilter.RecentlyUsed && !usageStatsRepository.isPermissionGranted.value -> AppDataPermission.UsageAccess
                    action.filter == QuickFilter.Unused && !usageStatsRepository.isPermissionGranted.value -> AppDataPermission.UsageAccess
                    action.filter == QuickFilter.Large && !storageStatsRepository.isPermissionGranted.value -> AppDataPermission.StorageAccess
                    else -> null
                }
                if (missingPermission != null) {
                    permissionRationale.value = missingPermission
                    return
                }
                appFilterRepository.toggleQuickFilter(action.filter)
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
