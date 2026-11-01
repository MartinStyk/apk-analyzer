package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.columns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import sk.styk.martin.apkanalyzer.core.apps.usagestats.UsageStatsRepository
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.InventoryExportStep
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.draft.InventoryExportDraft
import javax.inject.Inject

@HiltViewModel
class ColumnsStepViewModel @Inject constructor(private val draft: InventoryExportDraft, usageStatsRepository: UsageStatsRepository) : ViewModel() {

    private val eventChannel = Channel<ColumnsStepEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val state: StateFlow<ColumnsStepState> = combine(
        draft.attributes,
        usageStatsRepository.isPermissionGranted,
        ::ColumnsStepState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ColumnsStepState(selectedAttributes = draft.attributes.value))

    init {
        if (!draft.isStarted) eventChannel.trySend(ColumnsStepEvent.NavigateToStep(InventoryExportStep.Apps))
    }

    fun onAction(action: ColumnsStepAction) {
        when (action) {
            ColumnsStepAction.NavigateBack -> eventChannel.trySend(ColumnsStepEvent.NavigateBack)
            ColumnsStepAction.Continue -> if (state.value.canContinue) eventChannel.trySend(ColumnsStepEvent.NavigateToExport)
            is ColumnsStepAction.StepSelected -> eventChannel.trySend(ColumnsStepEvent.NavigateToStep(action.step))
            is ColumnsStepAction.AttributeToggled -> draft.toggleAttribute(action.attribute)
            is ColumnsStepAction.GroupToggled -> draft.toggleGroup(action.group)
            ColumnsStepAction.AllAttributesToggled -> draft.toggleAllAttributes()
            ColumnsStepAction.OpenUsageAccessSettings -> eventChannel.trySend(ColumnsStepEvent.OpenUsageAccessSettings)
        }
    }
}
