package sk.styk.martin.apkanalyzer.feature.pro.phone.analytics

import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsEvent
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsParameterName
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsTracker
import sk.styk.martin.apkanalyzer.core.entitlements.api.PurchaseResult
import sk.styk.martin.apkanalyzer.feature.pro.api.PaywallReason
import sk.styk.martin.apkanalyzer.feature.pro.api.WhatChangedPlacement
import javax.inject.Inject

internal class ProAnalytics @Inject constructor(private val analyticsTracker: AnalyticsTracker) {

    fun track(event: ProAnalyticsEvent) {
        val source = AnalyticsParameterName.PaywallSource to event.reason.analyticsValue
        analyticsTracker.track(
            when (event) {
                is ProAnalyticsEvent.Opened -> AnalyticsEvent("pro_paywall_opened", mapOf(source))

                is ProAnalyticsEvent.PurchaseStarted -> AnalyticsEvent("pro_purchase_started", mapOf(source))

                is ProAnalyticsEvent.PurchaseFinished -> AnalyticsEvent(
                    "pro_purchase_finished",
                    mapOf(source, AnalyticsParameterName.Result to event.result.analyticsValue),
                )

                is ProAnalyticsEvent.RestoreFinished -> AnalyticsEvent(
                    "pro_restore_finished",
                    mapOf(source, AnalyticsParameterName.Result to event.outcome.analyticsValue),
                )
            },
        )
    }
}

private val RestoreOutcome.analyticsValue: String
    get() = when (this) {
        RestoreOutcome.Restored -> "restored"
        RestoreOutcome.NothingFound -> "nothing_found"
        RestoreOutcome.Failed -> "failed"
    }

private val PaywallReason.analyticsValue: String
    get() = when (this) {
        is PaywallReason.Export -> "export"
        is PaywallReason.WhatChanged -> when (placement) {
            WhatChangedPlacement.LockedRow -> "what_changed_row"
            WhatChangedPlacement.Card -> "what_changed_card"
            WhatChangedPlacement.Bar -> "what_changed_bar"
            WhatChangedPlacement.LogRow -> "what_changed_log"
        }
        PaywallReason.Settings -> "settings"
        PaywallReason.Launch -> "launch_prompt"
    }

private val PurchaseResult.analyticsValue: String
    get() = when (this) {
        PurchaseResult.Purchased -> "purchased"
        PurchaseResult.Pending -> "pending"
        PurchaseResult.Cancelled -> "cancelled"
        PurchaseResult.Failed -> "failed"
    }
