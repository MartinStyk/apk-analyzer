package sk.styk.martin.apkanalyzer.core.entitlements.impl

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryProductDetailsResult
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import sk.styk.martin.apkanalyzer.core.common.coroutines.TestDispatcherProvider
import sk.styk.martin.apkanalyzer.core.entitlements.api.Entitlement
import sk.styk.martin.apkanalyzer.core.entitlements.api.Offer
import sk.styk.martin.apkanalyzer.core.entitlements.api.PurchaseResult
import sk.styk.martin.apkanalyzer.core.entitlements.api.RefreshResult
import sk.styk.martin.apkanalyzer.core.entitlements.impl.billing.BillingConnection
import sk.styk.martin.apkanalyzer.core.entitlements.impl.billing.PurchaseUpdates

@RunWith(RobolectricTestRunner::class)
class EntitlementsRepositoryImplTest {

    @get:Rule
    val dispatcherProvider = TestDispatcherProvider()

    private val billingClient = mockk<BillingClient> {
        every { isReady } returns true
        every { acknowledgePurchase(any(), any()) } answers {
            secondArg<AcknowledgePurchaseResponseListener>().onAcknowledgePurchaseResponse(aBillingResult(BillingResponseCode.OK))
        }
        every { queryProductDetailsAsync(any(), any()) } answers {
            secondArg<ProductDetailsResponseListener>().onProductDetailsResponse(
                aBillingResult(BillingResponseCode.OK),
                QueryProductDetailsResult.create(listOf(aProProduct(price = "€4.99")), emptyList()),
            )
        }
    }
    private val purchaseUpdates = PurchaseUpdates()
    private val repository = EntitlementsRepositoryImpl(billingClient, BillingConnection(billingClient), purchaseUpdates, dispatcherProvider)
    private val activity = mockk<Activity>()

    @Test
    fun `Given a pending purchase When observing entitlements Then nothing is granted`() = runTest {
        billingClient.givenPurchases(aProPurchase(state = Purchase.PurchaseState.PENDING))

        val entitlements = repository.entitlements().first()

        entitlements.shouldBeEmpty()
    }

    @Test
    fun `Given an unacknowledged purchase When observing entitlements Then it is granted and acknowledged`() = runTest {
        billingClient.givenPurchases(aProPurchase(acknowledged = false))

        val entitlements = repository.entitlements().first()

        entitlements shouldBe setOf(Entitlement.Pro)
        verify { billingClient.acknowledgePurchase(any(), any()) }
    }

    @Test
    fun `Given billing is unavailable When observing entitlements Then nothing is granted`() = runTest {
        billingClient.givenBillingUnavailable()

        val entitlements = repository.entitlements().first()

        entitlements.shouldBeEmpty()
    }

    @Test
    fun `Given a purchase completes while observing When observing entitlements Then Pro is granted`() = runTest {
        billingClient.givenPurchases()
        val entitlements = mutableListOf<Set<Entitlement>>()
        backgroundScope.launch(dispatcherProvider.default()) { repository.entitlements().toList(entitlements) }

        billingClient.givenPurchases(aProPurchase())
        purchaseUpdates.onPurchasesUpdated(aBillingResult(BillingResponseCode.OK), listOf(aProPurchase()))

        entitlements shouldBe listOf(emptySet(), setOf(Entitlement.Pro))
    }

    @Test
    fun `Given Pro is owned When a later purchase query fails Then Pro is kept`() = runTest {
        billingClient.givenPurchases(aProPurchase())
        val entitlements = mutableListOf<Set<Entitlement>>()
        backgroundScope.launch(dispatcherProvider.default()) { repository.entitlements().toList(entitlements) }
        every { billingClient.queryPurchasesAsync(any(), any()) } answers {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(aBillingResult(BillingResponseCode.SERVICE_UNAVAILABLE), emptyList())
        }

        purchaseUpdates.refresh()

        entitlements shouldBe listOf(setOf(Entitlement.Pro))
    }

    @Test
    fun `Given Play confirms a purchase When the ownership query then fails Then observers are granted Pro`() = runTest {
        billingClient.givenPurchases()
        val entitlements = mutableListOf<Set<Entitlement>>()
        backgroundScope.launch(dispatcherProvider.default()) { repository.entitlements().toList(entitlements) }
        every { billingClient.queryPurchasesAsync(any(), any()) } answers {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(aBillingResult(BillingResponseCode.SERVICE_UNAVAILABLE), emptyList())
        }

        purchaseUpdates.onPurchasesUpdated(aBillingResult(BillingResponseCode.OK), listOf(aProPurchase()))

        entitlements shouldBe listOf(emptySet(), setOf(Entitlement.Pro))
    }

    @Test
    fun `Given a purchase completes during the first query When observing entitlements Then Pro is granted`() = runTest {
        every { billingClient.queryPurchasesAsync(any(), any()) } answers {
            purchaseUpdates.onPurchasesUpdated(aBillingResult(BillingResponseCode.OK), listOf(aProPurchase()))
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(aBillingResult(BillingResponseCode.OK), emptyList())
        } andThenAnswer {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(aBillingResult(BillingResponseCode.OK), listOf(aProPurchase()))
        }
        val entitlements = mutableListOf<Set<Entitlement>>()

        backgroundScope.launch(dispatcherProvider.default()) { repository.entitlements().toList(entitlements) }

        entitlements shouldBe listOf(emptySet(), setOf(Entitlement.Pro))
    }

    @Test
    fun `Given Pro is not owned When observing offers Then Pro is offered at its price`() = runTest {
        billingClient.givenPurchases()

        val offers = repository.offers().first()

        offers shouldBe listOf(Offer(Entitlement.Pro, "€4.99"))
    }

    @Test
    fun `Given Pro is owned When observing offers Then nothing is offered`() = runTest {
        billingClient.givenPurchases(aProPurchase())

        val offers = repository.offers().first()

        offers.shouldBeEmpty()
    }

    @Test
    fun `Given billing is unavailable When observing offers Then nothing is offered`() = runTest {
        billingClient.givenBillingUnavailable()

        val offers = repository.offers().first()

        offers.shouldBeEmpty()
    }

    @Test
    fun `Given the billing flow completes When purchasing Then the purchase succeeds and is acknowledged`() = runTest {
        billingClient.givenBillingFlowUpdate(purchaseUpdates, BillingResponseCode.OK, aProPurchase(acknowledged = false))

        val result = repository.purchase(activity, Offer(Entitlement.Pro, "€4.99"))

        result shouldBe PurchaseResult.Purchased
        verify { billingClient.acknowledgePurchase(any(), any()) }
    }

    @Test
    fun `Given the payment is pending When purchasing Then the purchase is pending and not acknowledged`() = runTest {
        billingClient.givenBillingFlowUpdate(
            purchaseUpdates,
            BillingResponseCode.OK,
            aProPurchase(state = Purchase.PurchaseState.PENDING, acknowledged = false),
        )

        val result = repository.purchase(activity, Offer(Entitlement.Pro, "€4.99"))

        result shouldBe PurchaseResult.Pending
        verify(exactly = 0) { billingClient.acknowledgePurchase(any(), any()) }
    }

    @Test
    fun `Given the user cancels the billing flow When purchasing Then the purchase is cancelled`() = runTest {
        billingClient.givenBillingFlowUpdate(purchaseUpdates, BillingResponseCode.USER_CANCELED)

        val result = repository.purchase(activity, Offer(Entitlement.Pro, "€4.99"))

        result shouldBe PurchaseResult.Cancelled
    }

    @Test
    fun `Given Pro is already owned When purchasing Then the purchase succeeds and is acknowledged`() = runTest {
        billingClient.givenPurchases(aProPurchase(acknowledged = false))
        every { billingClient.launchBillingFlow(any(), any()) } returns aBillingResult(BillingResponseCode.ITEM_ALREADY_OWNED)

        val result = repository.purchase(activity, Offer(Entitlement.Pro, "€4.99"))

        result shouldBe PurchaseResult.Purchased
        verify { billingClient.acknowledgePurchase(any(), any()) }
    }

    @Test
    fun `Given stale entitlements When purchasing an already owned Pro Then observers are granted Pro`() = runTest {
        billingClient.givenPurchases()
        val entitlements = mutableListOf<Set<Entitlement>>()
        backgroundScope.launch(dispatcherProvider.default()) { repository.entitlements().toList(entitlements) }
        billingClient.givenPurchases(aProPurchase())
        every { billingClient.launchBillingFlow(any(), any()) } returns aBillingResult(BillingResponseCode.ITEM_ALREADY_OWNED)

        repository.purchase(activity, Offer(Entitlement.Pro, "€4.99"))

        entitlements shouldBe listOf(emptySet(), setOf(Entitlement.Pro))
    }

    @Test
    fun `Given Pro was bought elsewhere When refreshing Then Pro is returned and observers are granted Pro`() = runTest {
        billingClient.givenPurchases()
        val entitlements = mutableListOf<Set<Entitlement>>()
        backgroundScope.launch(dispatcherProvider.default()) { repository.entitlements().toList(entitlements) }
        every { billingClient.queryPurchasesAsync(any(), any()) } answers {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(aBillingResult(BillingResponseCode.OK), listOf(aProPurchase()))
        } andThenAnswer {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(aBillingResult(BillingResponseCode.SERVICE_UNAVAILABLE), emptyList())
        }

        val refreshed = repository.refresh()

        refreshed shouldBe RefreshResult.Refreshed(setOf(Entitlement.Pro))
        entitlements shouldBe listOf(emptySet(), setOf(Entitlement.Pro))
    }

    @Test
    fun `Given Play cannot be reached When refreshing Then the refresh fails`() = runTest {
        billingClient.givenBillingUnavailable()

        val refreshed = repository.refresh()

        refreshed shouldBe RefreshResult.Failed
    }

    @Test
    fun `Given the billing flow fails to open When purchasing Then the purchase fails`() = runTest {
        every { billingClient.launchBillingFlow(any(), any()) } returns aBillingResult(BillingResponseCode.SERVICE_UNAVAILABLE)

        val result = repository.purchase(activity, Offer(Entitlement.Pro, "€4.99"))

        result shouldBe PurchaseResult.Failed
    }

    @Test
    fun `Given billing is unavailable When purchasing Then the purchase fails`() = runTest {
        billingClient.givenBillingUnavailable()

        val result = repository.purchase(activity, Offer(Entitlement.Pro, "€4.99"))

        result shouldBe PurchaseResult.Failed
    }
}

private fun BillingClient.givenPurchases(vararg purchases: Purchase) {
    every { queryPurchasesAsync(any(), any()) } answers {
        secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(aBillingResult(BillingResponseCode.OK), purchases.toList())
    }
}

private fun BillingClient.givenBillingUnavailable() {
    every { isReady } returns false
    every { startConnection(any()) } answers {
        firstArg<BillingClientStateListener>().onBillingSetupFinished(aBillingResult(BillingResponseCode.BILLING_UNAVAILABLE))
    }
}

private fun BillingClient.givenBillingFlowUpdate(
    updates: PurchaseUpdates,
    responseCode: Int,
    vararg purchases: Purchase,
) {
    every { launchBillingFlow(any(), any()) } answers {
        updates.onPurchasesUpdated(aBillingResult(responseCode), purchases.toList())
        aBillingResult(BillingResponseCode.OK)
    }
}

private fun aProPurchase(state: Int = Purchase.PurchaseState.PURCHASED, acknowledged: Boolean = true): Purchase = mockk {
    every { products } returns listOf("todo")
    every { purchaseState } returns state
    every { isAcknowledged } returns acknowledged
    every { purchaseToken } returns "purchase-token"
}

private fun aProProduct(price: String): ProductDetails = ProductDetails::class.java
    .getDeclaredConstructor(String::class.java)
    .apply { isAccessible = true }
    .newInstance(
        """
        {
          "productId": "todo",
          "type": "inapp",
          "title": "Pro (Apk Analyzer)",
          "name": "Pro",
          "description": "Unlocks Pro",
          "packageName": "sk.styk.martin.apkanalyzer",
          "skuDetailsToken": "sku-details-token",
          "oneTimePurchaseOfferDetails": {
            "priceAmountMicros": 4990000,
            "priceCurrencyCode": "EUR",
            "formattedPrice": "$price",
            "offerIdToken": "offer-token"
          }
        }
        """.trimIndent(),
    )

private fun aBillingResult(responseCode: Int): BillingResult = BillingResult.newBuilder().setResponseCode(responseCode).build()
