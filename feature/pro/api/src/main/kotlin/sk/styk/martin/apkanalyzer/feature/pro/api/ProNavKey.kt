package sk.styk.martin.apkanalyzer.feature.pro.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ProNavKey(val reason: PaywallReason) : NavKey

@Serializable
sealed interface PaywallReason {
    @Serializable
    data class Export(val appCount: Int) : PaywallReason

    @Serializable
    data class WhatChanged(val lockedAppCount: Int) : PaywallReason
}
