package sk.styk.martin.apkanalyzer.feature.pro.phone.support

import java.time.Duration
import java.time.Instant
import javax.inject.Inject

private const val MAX_TIMES_SHOWN = 2
private val MIN_INSTALL_AGE = Duration.ofDays(7)
private val MIN_TIME_SINCE_REVIEW = Duration.ofDays(14)

internal data class SupportPromptInput(
    val now: Instant,
    val installedAt: Instant,
    val versionCode: Long,
    val lastShownVersionCode: Long,
    val timesShown: Int,
    val hasAccomplishment: Boolean,
    val hasPro: Boolean,
    val isPriceAvailable: Boolean,
    val paywallShownThisSession: Boolean,
    val reviewRequestedAt: Instant?,
)

internal class SupportPromptPolicy @Inject constructor() {

    fun shouldShow(input: SupportPromptInput): Boolean = with(input) {
        hasAccomplishment &&
            !hasPro &&
            isPriceAvailable &&
            !paywallShownThisSession &&
            timesShown < MAX_TIMES_SHOWN &&
            versionCode > lastShownVersionCode &&
            !installedAt.plus(MIN_INSTALL_AGE).isAfter(now) &&
            (reviewRequestedAt == null || !reviewRequestedAt.plus(MIN_TIME_SINCE_REVIEW).isAfter(now))
    }
}
