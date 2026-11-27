package sk.styk.martin.apkanalyzer.core.entitlements.impl.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "BillingConnection"

@Singleton
internal class BillingConnection @Inject constructor(private val billingClient: BillingClient) {
    private val mutex = Mutex()

    suspend fun connect(): Boolean = mutex.withLock {
        if (billingClient.isReady) return@withLock true
        val result = startConnection()
        if (result.responseCode != BillingResponseCode.OK) {
            Logger.w(TAG, "Billing setup failed with response code ${result.responseCode}")
        }
        result.responseCode == BillingResponseCode.OK
    }

    private suspend fun startConnection(): BillingResult = suspendCancellableCoroutine { continuation ->
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (continuation.isActive) {
                        continuation.resume(billingResult)
                    }
                }

                override fun onBillingServiceDisconnected() = Unit
            },
        )
    }
}
