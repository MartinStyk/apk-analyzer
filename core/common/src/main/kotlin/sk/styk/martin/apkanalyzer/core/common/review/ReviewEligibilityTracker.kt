package sk.styk.martin.apkanalyzer.core.common.review

import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ReviewEligibilityTracker {
    val reviewPromptRequests: Flow<Unit>

    fun recordAppDetailSessionCompleted(
        startTime: Instant,
        endTime: Instant,
        engaged: Boolean,
    )

    fun recordPromptShown()
}
