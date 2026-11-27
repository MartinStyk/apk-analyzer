package sk.styk.martin.apkanalyzer.core.entitlements.api

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class EntitlementsRepositoryFake(vararg entitlements: Entitlement) : EntitlementsRepository {
    private val owned = MutableStateFlow(entitlements.toSet())
    private val available = MutableStateFlow<List<Offer>>(emptyList())

    var purchaseResult: PurchaseResult = PurchaseResult.Purchased

    operator fun plusAssign(entitlement: Entitlement) {
        owned.update { current -> current + entitlement }
    }

    operator fun plusAssign(offer: Offer) {
        available.update { current -> current.filterNot { it.entitlement == offer.entitlement } + offer }
    }

    override fun entitlements(): Flow<Set<Entitlement>> = owned

    override fun offers(): Flow<List<Offer>> = combine(available, owned) { offers, entitlements ->
        offers.filterNot { it.entitlement in entitlements }
    }

    override suspend fun purchase(activity: Activity, offer: Offer): PurchaseResult {
        if (purchaseResult == PurchaseResult.Purchased) {
            this += offer.entitlement
        }
        return purchaseResult
    }
}
