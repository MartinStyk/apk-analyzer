package sk.styk.martin.apkanalyzer.feature.pro.phone

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryProSummary
import sk.styk.martin.apkanalyzer.core.entitlements.api.Offer
import sk.styk.martin.apkanalyzer.feature.pro.api.PaywallReason

@Immutable
internal data class ProState(
    val reason: PaywallReason,
    val offer: ProOffer = ProOffer.Loading,
    val status: ProStatus = ProStatus.Idle,
    val history: ProHistory = ProHistory.Loading,
)

@Immutable
internal sealed interface ProOffer {
    data object Loading : ProOffer

    data class Available(val offer: Offer) : ProOffer

    data object Unavailable : ProOffer
}

@Immutable
internal sealed interface ProStatus {
    data object Idle : ProStatus

    data object Purchasing : ProStatus

    data object Pending : ProStatus

    data object Failed : ProStatus

    data object Restoring : ProStatus

    data object NothingToRestore : ProStatus
}

@Immutable
internal sealed interface ProHistory {
    data object Loading : ProHistory

    data object Unavailable : ProHistory

    data class Available(val summary: AppHistoryProSummary) : ProHistory
}

internal val ProHistory.summary: AppHistoryProSummary?
    get() = (this as? ProHistory.Available)?.summary

internal val ProStatus.blocksActions: Boolean
    get() = this == ProStatus.Purchasing || this == ProStatus.Restoring || this == ProStatus.Pending
