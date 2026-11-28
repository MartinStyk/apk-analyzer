package sk.styk.martin.apkanalyzer.feature.pro.phone

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryDeviceTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.proSummary
import sk.styk.martin.apkanalyzer.core.entitlements.api.Entitlement
import sk.styk.martin.apkanalyzer.core.entitlements.api.EntitlementsRepository
import sk.styk.martin.apkanalyzer.core.entitlements.api.PurchaseResult
import sk.styk.martin.apkanalyzer.core.entitlements.api.RefreshResult
import sk.styk.martin.apkanalyzer.feature.pro.api.PaywallReason
import sk.styk.martin.apkanalyzer.feature.pro.api.ProSheetResult
import sk.styk.martin.apkanalyzer.feature.pro.phone.analytics.ProAnalytics
import sk.styk.martin.apkanalyzer.feature.pro.phone.analytics.ProAnalyticsEvent
import sk.styk.martin.apkanalyzer.feature.pro.phone.analytics.RestoreOutcome
import sk.styk.martin.apkanalyzer.feature.pro.phone.support.SupportPromptSession

@HiltViewModel(assistedFactory = ProViewModel.Factory::class)
internal class ProViewModel @AssistedInject constructor(
    @Assisted private val reason: PaywallReason,
    private val entitlementsRepository: EntitlementsRepository,
    private val appHistoryRepository: AppHistoryRepository,
    private val supportPromptSession: SupportPromptSession,
    private val analytics: ProAnalytics,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(reason: PaywallReason): ProViewModel
    }

    val state: StateFlow<ProState>
        field = MutableStateFlow(ProState(reason = reason))

    private val eventChannel = Channel<ProEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var isClosing = false

    init {
        analytics.track(ProAnalyticsEvent.Opened(reason))
        supportPromptSession.recordPaywallShown()
        viewModelScope.launch {
            entitlementsRepository.entitlements().first { Entitlement.Pro in it }
            close(ProEvent.Close)
        }
        viewModelScope.launch {
            entitlementsRepository.offers().collect { offers ->
                val offer = offers.firstOrNull { it.entitlement == Entitlement.Pro }?.let(ProOffer::Available) ?: ProOffer.Unavailable
                state.update { it.copy(offer = offer) }
            }
        }
        viewModelScope.launch {
            appHistoryRepository.deviceTimeline().collect { timeline ->
                val history = when (timeline) {
                    AppHistoryDeviceTimeline.Unavailable -> ProHistory.Unavailable
                    is AppHistoryDeviceTimeline.Available -> timeline.proSummary()?.let(ProHistory::Available) ?: ProHistory.Unavailable
                }
                state.update { it.copy(history = history) }
            }
        }
    }

    fun onAction(action: ProAction) {
        when (action) {
            is ProAction.Unlock -> unlock(action.activity)
            ProAction.Restore -> restore()
            ProAction.ExportBasicsOnly -> close(ProEvent.CloseWithResult(ProSheetResult.ExportBasicsOnly))
            ProAction.Dismiss -> close(ProEvent.Close)
        }
    }

    private fun unlock(activity: Activity) {
        val offer = (state.value.offer as? ProOffer.Available)?.offer ?: return
        if (state.value.status.blocksActions) return
        state.update { it.copy(status = ProStatus.Purchasing) }
        analytics.track(ProAnalyticsEvent.PurchaseStarted(reason))
        viewModelScope.launch {
            val result = entitlementsRepository.purchase(activity, offer)
            analytics.track(ProAnalyticsEvent.PurchaseFinished(reason, result))
            when (result) {
                PurchaseResult.Purchased -> close(ProEvent.Close)
                PurchaseResult.Pending -> state.update { it.copy(status = ProStatus.Pending) }
                PurchaseResult.Cancelled -> state.update { it.copy(status = ProStatus.Idle) }
                PurchaseResult.Failed -> state.update { it.copy(status = ProStatus.Failed) }
            }
        }
    }

    private fun restore() {
        if (state.value.status.blocksActions) return
        state.update { it.copy(status = ProStatus.Restoring) }
        viewModelScope.launch {
            val outcome = when (val refreshed = entitlementsRepository.refresh()) {
                is RefreshResult.Refreshed -> if (Entitlement.Pro in refreshed.owned) RestoreOutcome.Restored else RestoreOutcome.NothingFound
                RefreshResult.Failed -> RestoreOutcome.Failed
            }
            analytics.track(ProAnalyticsEvent.RestoreFinished(reason, outcome))
            when (outcome) {
                RestoreOutcome.Restored -> close(ProEvent.Close)
                RestoreOutcome.NothingFound -> state.update { it.copy(status = ProStatus.NothingToRestore) }
                RestoreOutcome.Failed -> state.update { it.copy(status = ProStatus.Failed) }
            }
        }
    }

    private fun close(event: ProEvent) {
        if (isClosing) return
        isClosing = true
        eventChannel.trySend(event)
    }
}
