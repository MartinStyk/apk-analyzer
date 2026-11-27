package sk.styk.martin.apkanalyzer.core.entitlements.api

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

interface EntitlementsRepository {
    fun entitlements(): Flow<Set<Entitlement>>

    fun offers(): Flow<List<Offer>>

    suspend fun purchase(activity: Activity, offer: Offer): PurchaseResult
}

@Serializable
enum class Entitlement {
    Pro,
}

@Serializable
data class Offer(val entitlement: Entitlement, val formattedPrice: String)

sealed interface PurchaseResult {
    data object Purchased : PurchaseResult

    data object Pending : PurchaseResult

    data object Cancelled : PurchaseResult

    data object Failed : PurchaseResult
}
