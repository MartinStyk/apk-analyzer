package sk.styk.martin.apkanalyzer.core.entitlements.impl.billing

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import sk.styk.martin.apkanalyzer.core.entitlements.api.Entitlement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PurchaseUpdates @Inject constructor() : PurchasesUpdatedListener {
    val updates: SharedFlow<PurchaseUpdate>
        field = MutableSharedFlow(extraBufferCapacity = UPDATE_BUFFER_CAPACITY)

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        updates.tryEmit(PurchaseUpdate.FromPlay(billingResult.responseCode, purchases.orEmpty()))
    }

    fun refresh() {
        updates.tryEmit(PurchaseUpdate.Refresh)
    }

    fun publish(owned: Set<Entitlement>) {
        updates.tryEmit(PurchaseUpdate.Known(owned))
    }

    private companion object {
        const val UPDATE_BUFFER_CAPACITY = 8
    }
}

internal sealed interface PurchaseUpdate {
    data class FromPlay(val responseCode: Int, val purchases: List<Purchase>) : PurchaseUpdate

    data object Refresh : PurchaseUpdate

    data class Known(val owned: Set<Entitlement>) : PurchaseUpdate
}
