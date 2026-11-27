package sk.styk.martin.apkanalyzer.feature.pro.phone

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.feature.pro.api.PaywallReason

@Immutable
internal data class ProState(
    val reason: PaywallReason,
    val offer: ProOffer = ProOffer.Loading,
    val status: ProStatus = ProStatus.Idle,
)

@Immutable
internal sealed interface ProOffer {
    data object Loading : ProOffer

    data class Available(val formattedPrice: String) : ProOffer

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

internal val ProStatus.blocksActions: Boolean
    get() = this == ProStatus.Purchasing || this == ProStatus.Restoring || this == ProStatus.Pending
