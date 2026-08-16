package sk.styk.martin.apkanalyzer.core.common.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject

internal class FirebaseAnalyticsTracker @Inject constructor(private val analytics: FirebaseAnalytics) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        analytics.logEvent(
            event.name,
            Bundle().apply {
                event.parameters.forEach { (name, value) -> putString(name.value, value) }
            },
        )
    }
}
