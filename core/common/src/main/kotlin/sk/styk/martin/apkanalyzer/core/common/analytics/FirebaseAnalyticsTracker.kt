package sk.styk.martin.apkanalyzer.core.common.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class FirebaseAnalyticsTracker @Inject constructor(
    @ApplicationContext context: Context,
) : AnalyticsTracker {
    private val analytics = FirebaseAnalytics.getInstance(context)

    override fun track(event: AnalyticsEvent) {
        analytics.logEvent(
            event.name,
            Bundle().apply {
                event.parameters.forEach { (name, value) -> putString(name, value) }
            },
        )
    }
}
