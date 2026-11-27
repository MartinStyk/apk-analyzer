package sk.styk.martin.apkanalyzer.core.entitlements.impl.billing

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject

internal class ForegroundPurchaseRefresh @Inject constructor(private val purchaseUpdates: PurchaseUpdates) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        purchaseUpdates.refresh()
    }
}
