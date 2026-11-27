package sk.styk.martin.apkanalyzer.core.entitlements.noop

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import sk.styk.martin.apkanalyzer.core.entitlements.api.Entitlement
import sk.styk.martin.apkanalyzer.core.entitlements.api.EntitlementsRepository
import sk.styk.martin.apkanalyzer.core.entitlements.api.Offer
import sk.styk.martin.apkanalyzer.core.entitlements.api.PurchaseResult
import javax.inject.Inject

internal class EntitlementsRepositoryImpl @Inject constructor() : EntitlementsRepository {

    override fun entitlements(): Flow<Set<Entitlement>> = flowOf(Entitlement.entries.toSet())

    override fun offers(): Flow<List<Offer>> = flowOf(emptyList())

    override suspend fun purchase(activity: Activity, offer: Offer): PurchaseResult = PurchaseResult.Failed

    override suspend fun refresh(): Set<Entitlement> = Entitlement.entries.toSet()
}
