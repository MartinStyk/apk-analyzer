package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.draft.InventoryExportDraft
import javax.inject.Inject

@HiltViewModel
class AppsStepViewModel @Inject constructor(private val draft: InventoryExportDraft, dispatcherProvider: DispatcherProvider) : ViewModel() {

    private val eventChannel = Channel<AppsStepEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        draft.startFromListFilter()
    }

    val state: StateFlow<AppsStepState> = combine(draft.apps, draft.filter) { apps, filter ->
        AppsStepState(
            apps = InventoryAppList.Content(apps.map { InventoryAppItem(it.packageName, it.applicationName) }),
            isFilterActive = filter.isActive,
        )
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsStepState())

    fun onAction(action: AppsStepAction) {
        when (action) {
            AppsStepAction.NavigateBack -> eventChannel.trySend(AppsStepEvent.NavigateBack)
            AppsStepAction.Continue -> if (state.value.canContinue) eventChannel.trySend(AppsStepEvent.NavigateToColumns)
            AppsStepAction.EditFilters -> eventChannel.trySend(AppsStepEvent.NavigateToFilter)
            AppsStepAction.IncludeAllApps -> draft.includeAllApps()
            is AppsStepAction.AppClicked -> eventChannel.trySend(AppsStepEvent.NavigateToAppDetail(action.packageName))
        }
    }
}
