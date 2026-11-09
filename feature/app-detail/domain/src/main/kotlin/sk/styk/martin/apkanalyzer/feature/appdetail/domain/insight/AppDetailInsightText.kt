package sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight

import android.content.res.Resources
import androidx.annotation.StringRes
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.R

fun AppDetailInsight.label(resources: Resources): String = when (this) {
    AppDetailInsight.Debuggable -> resources.getString(R.string.app_detail_insight_debuggable)

    AppDetailInsight.DebugCertificate -> resources.getString(R.string.app_detail_insight_debug_certificate)

    AppDetailInsight.CertificateNotYetValid -> resources.getString(R.string.app_detail_insight_certificate_not_yet_valid)

    AppDetailInsight.Sideloaded -> resources.getString(R.string.app_detail_insight_sideloaded)

    is AppDetailInsight.OutdatedTargetSdk -> resources.getQuantityString(
        R.plurals.app_detail_insight_outdated_target,
        apiLevelGap,
        targetSdk,
        apiLevelGap,
    )

    is AppDetailInsight.Unused -> resources.getQuantityString(
        R.plurals.app_detail_insight_unused,
        monthsSinceLastUsed,
        monthsSinceLastUsed,
    )

    is AppDetailInsight.SensitivePermission -> resources.getString(
        when (access) {
            SensitiveAccess.BackgroundLocation -> R.string.app_detail_insight_background_location
            SensitiveAccess.Messages -> R.string.app_detail_insight_messages
            SensitiveAccess.CallHistory -> R.string.app_detail_insight_call_history
            SensitiveAccess.Contacts -> R.string.app_detail_insight_contacts
            SensitiveAccess.Calendar -> R.string.app_detail_insight_calendar
        },
    )
}

@get:StringRes
val AppDetailBadge.labelRes: Int
    get() = when (this) {
        AppDetailBadge.Large -> R.string.app_detail_badge_large
        AppDetailBadge.System -> R.string.app_detail_badge_system
        AppDetailBadge.GooglePlay -> R.string.app_detail_badge_google_play
        AppDetailBadge.RecentlyInstalled -> R.string.app_detail_badge_recently_installed
        AppDetailBadge.RecentlyUpdated -> R.string.app_detail_badge_recently_updated
        AppDetailBadge.RecentlyUsed -> R.string.app_detail_badge_recently_used
    }
