package sk.styk.martin.apkanalyzer.core.apps

import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import java.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

object AppClassificationThresholds {
    val LARGE_SIZE = 500.megabytes
    val RECENT_PERIOD: Duration = 30.days.toJavaDuration()
    const val RECENTLY_USED_DAYS = 2
    val UNUSED_PERIOD: Duration = 180.days.toJavaDuration()
}
