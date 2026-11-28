package sk.styk.martin.apkanalyzer.feature.whatchanged.phone

import androidx.annotation.StringRes
import java.time.Duration
import java.time.Instant

internal enum class WhatChangedRange(@param:StringRes val labelRes: Int) {
    SinceLastVisit(R.string.what_changed_range_since_last_visit),
    Last7Days(R.string.what_changed_range_7_days),
    Last30Days(R.string.what_changed_range_30_days),
    Last90Days(R.string.what_changed_range_90_days),
    AllTime(R.string.what_changed_range_all_time),
    ;

    fun since(
        now: Instant,
        previousVisit: Instant?,
        recordingSince: Instant,
    ): Instant = when (this) {
        SinceLastVisit -> previousVisit ?: recordingSince
        Last7Days -> now.minus(Duration.ofDays(7))
        Last30Days -> now.minus(Duration.ofDays(30))
        Last90Days -> now.minus(Duration.ofDays(90))
        AllTime -> recordingSince
    }

    companion object {
        fun initial(storedName: String, hasPro: Boolean): WhatChangedRange = when {
            hasPro -> entries.firstOrNull { it.name == storedName } ?: SinceLastVisit
            else -> AllTime
        }
    }
}
