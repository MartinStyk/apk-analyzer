package sk.styk.martin.apkanalyzer.core.common.review

import kotlinx.coroutines.flow.Flow

interface ReviewEligibilityTracker {
    val reviewPromptRequests: Flow<Unit>

    fun recordAppDetailSessionCompleted(qualified: Boolean)

    fun recordPromptShown()
}
