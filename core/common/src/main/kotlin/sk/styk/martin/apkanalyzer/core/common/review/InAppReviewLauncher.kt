package sk.styk.martin.apkanalyzer.core.common.review

import android.app.Activity

interface InAppReviewLauncher {
    suspend fun launchReviewFlow(activity: Activity): Result<Unit>
}
