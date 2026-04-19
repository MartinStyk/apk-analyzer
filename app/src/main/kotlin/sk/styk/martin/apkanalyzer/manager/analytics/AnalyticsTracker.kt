package sk.styk.martin.apkanalyzer.manager.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject

private const val APP_ACTION = "apk-action"

class AnalyticsTracker
@Inject
constructor(private val firebaseAnalytics: FirebaseAnalytics) {
    enum class AppAction(val trackingId: String) {
        SHOW_MANIFEST("show-manifest"),
        EXPORT_APK("export-apk"),
        SAVE_ICON("save-icon"),
        OPEN_SYSTEM_ABOUT("open-system-about"),
        OPEN_GOOGLE_PLAY("open-google-play"),
    }

    fun trackAppActionAction(action: AppAction) {
        val bundle =
            Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, action.trackingId)
                putString(FirebaseAnalytics.Param.CONTENT_TYPE, APP_ACTION)
            }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    fun trackScreenView(screenTag: String) {
        val bundle =
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenTag)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenTag)
            }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
}
