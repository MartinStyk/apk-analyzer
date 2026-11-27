package sk.styk.martin.apkanalyzer.core.entitlements.impl

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.entitlements.api.Entitlement
import sk.styk.martin.apkanalyzer.core.entitlements.api.EntitlementsRepository
import sk.styk.martin.apkanalyzer.core.entitlements.api.Offer
import sk.styk.martin.apkanalyzer.core.entitlements.api.PurchaseResult
import sk.styk.martin.apkanalyzer.core.entitlements.api.RefreshResult
import sk.styk.martin.apkanalyzer.core.entitlements.impl.billing.BillingConnection
import sk.styk.martin.apkanalyzer.core.entitlements.impl.billing.PurchaseUpdate
import sk.styk.martin.apkanalyzer.core.entitlements.impl.billing.PurchaseUpdates
import javax.inject.Inject

private const val TAG = "Entitlements"
private const val PRO_PRODUCT_ID = "todo"

internal class EntitlementsRepositoryImpl @Inject constructor(
    private val billingClient: BillingClient,
    private val billingConnection: BillingConnection,
    private val purchaseUpdates: PurchaseUpdates,
    private val dispatcherProvider: DispatcherProvider,
) : EntitlementsRepository {

    override fun entitlements(): Flow<Set<Entitlement>> = purchaseUpdates.updates
        .onSubscription { emit(PurchaseUpdate.Refresh) }
        .map { update ->
            when (update) {
                is PurchaseUpdate.Known -> RefreshResult.Refreshed(update.owned)
                is PurchaseUpdate.FromPlay, PurchaseUpdate.Refresh -> queryOwnedEntitlements()
            }
        }
        .keepingLastKnown()
        .distinctUntilChanged()

    override fun offers(): Flow<List<Offer>> = combine(
        flow { emit(queryOffers()) },
        entitlements(),
    ) { offers, owned ->
        offers.filterNot { it.entitlement in owned }
    }

    override suspend fun purchase(activity: Activity, offer: Offer): PurchaseResult {
        val productDetails = queryProductDetails().firstOrNull { it.productId == offer.entitlement.productId }
            ?: return PurchaseResult.Failed
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).build()),
            )
            .build()
        return coroutineScope {
            val update = async(start = CoroutineStart.UNDISPATCHED) {
                purchaseUpdates.updates.filterIsInstance<PurchaseUpdate.FromPlay>().first { it.concerns(offer.entitlement) }
            }
            val launchResult = withContext(dispatcherProvider.main()) {
                billingClient.launchBillingFlow(activity, params)
            }
            if (launchResult.responseCode == BillingResponseCode.OK) {
                purchaseResultOf(update.await(), offer.entitlement)
            } else {
                update.cancel()
                if (launchResult.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) {
                    refresh()
                }
                purchaseResultOf(launchResult.responseCode)
            }
        }
    }

    override suspend fun refresh(): RefreshResult = queryOwnedEntitlements().also { if (it is RefreshResult.Refreshed) purchaseUpdates.publish(it.owned) }

    private suspend fun queryOwnedEntitlements(): RefreshResult {
        if (!billingConnection.connect()) return RefreshResult.Failed
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode != BillingResponseCode.OK) {
            Logger.w(TAG, "Querying purchases failed with response code ${result.billingResult.responseCode}")
            return RefreshResult.Failed
        }
        val purchased = result.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        purchased.forEach { acknowledge(it) }
        return RefreshResult.Refreshed(purchased.flatMap { it.products }.mapNotNull(::entitlementOf).toSet())
    }

    private suspend fun queryOffers(): List<Offer> = queryProductDetails().mapNotNull { details ->
        val entitlement = entitlementOf(details.productId)
        val formattedPrice = details.oneTimePurchaseOfferDetails?.formattedPrice
        if (entitlement != null && formattedPrice != null) Offer(entitlement, formattedPrice) else null
    }

    private suspend fun queryProductDetails(): List<ProductDetails> {
        if (!billingConnection.connect()) return emptyList()
        val products = Entitlement.entries.map { entitlement ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(entitlement.productId)
                .setProductType(ProductType.INAPP)
                .build()
        }
        val result = billingClient.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(products).build())
        if (result.billingResult.responseCode != BillingResponseCode.OK) {
            Logger.w(TAG, "Querying product details failed with response code ${result.billingResult.responseCode}")
            return emptyList()
        }
        return result.productDetailsList.orEmpty()
    }

    private suspend fun purchaseResultOf(update: PurchaseUpdate.FromPlay, entitlement: Entitlement): PurchaseResult {
        if (update.responseCode != BillingResponseCode.OK) return purchaseResultOf(update.responseCode)
        val purchase = update.purchases.firstOrNull { entitlement.productId in it.products } ?: return PurchaseResult.Failed
        return when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                acknowledge(purchase)
                PurchaseResult.Purchased
            }

            Purchase.PurchaseState.PENDING -> PurchaseResult.Pending

            else -> PurchaseResult.Failed
        }
    }

    private fun purchaseResultOf(responseCode: Int): PurchaseResult = when (responseCode) {
        BillingResponseCode.USER_CANCELED -> PurchaseResult.Cancelled

        BillingResponseCode.ITEM_ALREADY_OWNED -> PurchaseResult.Purchased

        else -> {
            Logger.w(TAG, "Purchase failed with response code $responseCode")
            PurchaseResult.Failed
        }
    }

    private suspend fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        val result = billingClient.acknowledgePurchase(params)
        if (result.responseCode != BillingResponseCode.OK) {
            Logger.w(TAG, "Acknowledging purchase failed with response code ${result.responseCode}")
        }
    }
}

private fun Flow<RefreshResult>.keepingLastKnown(): Flow<Set<Entitlement>> = flow {
    var lastKnown = emptySet<Entitlement>()
    collect { queried ->
        if (queried is RefreshResult.Refreshed) lastKnown = queried.owned
        emit(lastKnown)
    }
}

private fun PurchaseUpdate.FromPlay.concerns(entitlement: Entitlement): Boolean =
    responseCode != BillingResponseCode.OK || purchases.any { entitlement.productId in it.products }

private val Entitlement.productId: String
    get() = when (this) {
        Entitlement.Pro -> PRO_PRODUCT_ID
    }

private fun entitlementOf(productId: String): Entitlement? = Entitlement.entries.firstOrNull { it.productId == productId }
