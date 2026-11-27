package sk.styk.martin.apkanalyzer.core.entitlements.impl.di

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import sk.styk.martin.apkanalyzer.core.entitlements.api.EntitlementsRepository
import sk.styk.martin.apkanalyzer.core.entitlements.impl.EntitlementsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.entitlements.impl.billing.ForegroundPurchaseRefresh
import sk.styk.martin.apkanalyzer.core.entitlements.impl.billing.PurchaseUpdates
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface EntitlementsModule {
    @Binds
    @Singleton
    fun bindEntitlementsRepository(impl: EntitlementsRepositoryImpl): EntitlementsRepository

    @Binds
    @IntoSet
    fun bindForegroundPurchaseRefresh(impl: ForegroundPurchaseRefresh): DefaultLifecycleObserver

    companion object {
        @Provides
        @Singleton
        fun provideBillingClient(@ApplicationContext context: Context, purchaseUpdates: PurchaseUpdates): BillingClient = BillingClient.newBuilder(context)
            .setListener(purchaseUpdates)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
    }
}
