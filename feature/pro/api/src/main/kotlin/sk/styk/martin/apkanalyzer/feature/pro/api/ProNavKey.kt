package sk.styk.martin.apkanalyzer.feature.pro.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ProNavKey(val reason: PaywallReason) : NavKey

@Serializable
sealed interface PaywallReason {
    @Serializable
    data class Export(
        val appCount: Int,
        val sampleAppNames: List<String>,
        val freeDetailCount: Int,
        val proDetailCount: Int,
    ) : PaywallReason

    @Serializable
    data class WhatChanged(val placement: WhatChangedPlacement) : PaywallReason

    @Serializable
    data object Settings : PaywallReason

    @Serializable
    data object Launch : PaywallReason
}

@Serializable
enum class WhatChangedPlacement {
    LockedRow,
    Card,
    Bar,
    LogRow,
}

@Serializable
sealed interface ProSheetResult {
    @Serializable
    data object ExportBasicsOnly : ProSheetResult
}
