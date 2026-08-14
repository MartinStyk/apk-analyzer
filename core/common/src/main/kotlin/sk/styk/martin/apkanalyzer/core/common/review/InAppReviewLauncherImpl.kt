package sk.styk.martin.apkanalyzer.core.common.review

import android.app.Activity
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import kotlinx.coroutines.suspendCancellableCoroutine
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "InAppReviewLauncher"

internal class InAppReviewLauncherImpl @Inject constructor(
    private val reviewManager: ReviewManager,
    private val reviewEligibilityTracker: ReviewEligibilityTracker,
) : InAppReviewLauncher {

    override suspend fun launchReviewFlow(activity: Activity): Result<Unit> {
        val result = runCatchingCancellable {
            val reviewInfo = requestReviewInfo()
            launchFlow(activity, reviewInfo)
        }
        reviewEligibilityTracker.recordPromptShown()
        return result
    }

    private suspend fun requestReviewInfo(): ReviewInfo = suspendCancellableCoroutine { continuation ->
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: IllegalStateException("Unable to request review flow"))
            }
        }
    }

    private suspend fun launchFlow(activity: Activity, reviewInfo: ReviewInfo): Unit = suspendCancellableCoroutine { continuation ->
        reviewManager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener {
            continuation.resume(Unit)
        }
    }
}
