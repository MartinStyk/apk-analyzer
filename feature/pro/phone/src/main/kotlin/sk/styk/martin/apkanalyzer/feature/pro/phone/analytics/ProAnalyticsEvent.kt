package sk.styk.martin.apkanalyzer.feature.pro.phone.analytics

import sk.styk.martin.apkanalyzer.core.entitlements.api.PurchaseResult
import sk.styk.martin.apkanalyzer.feature.pro.api.PaywallReason

internal sealed interface ProAnalyticsEvent {
    val reason: PaywallReason

    data class Opened(override val reason: PaywallReason) : ProAnalyticsEvent

    data class PurchaseStarted(override val reason: PaywallReason) : ProAnalyticsEvent

    data class PurchaseFinished(override val reason: PaywallReason, val result: PurchaseResult) : ProAnalyticsEvent

    data class RestoreFinished(override val reason: PaywallReason, val restored: Boolean) : ProAnalyticsEvent
}
