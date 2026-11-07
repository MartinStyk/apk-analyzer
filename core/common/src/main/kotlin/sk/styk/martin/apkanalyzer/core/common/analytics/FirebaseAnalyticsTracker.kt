package sk.styk.martin.apkanalyzer.core.common.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject

private const val FORM_FACTOR_USER_PROPERTY = "form_factor"

internal class FirebaseAnalyticsTracker @Inject constructor(private val analytics: FirebaseAnalytics) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        analytics.logEvent(
            event.name,
            Bundle().apply {
                event.parameters.forEach { (name, value) -> putString(name.value, value) }
            },
        )
    }

    override fun setFormFactor(formFactor: AnalyticsFormFactor) {
        analytics.setUserProperty(FORM_FACTOR_USER_PROPERTY, formFactor.value)
    }
}
