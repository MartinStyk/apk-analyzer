package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import android.content.Context
import androidx.annotation.PluralsRes
import sk.styk.martin.apkanalyzer.core.apps.model.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

internal fun AppDetailFeedback.message(context: Context): String = when (this) {
    is AppDetailFeedback.ApkSaved -> context.getString(R.string.app_detail_apk_saved, displayName)
    is AppDetailFeedback.IconSaved -> context.getString(R.string.app_detail_icon_saved, displayName)
    AppDetailFeedback.ApkSaveFailed -> context.getString(R.string.app_detail_apk_save_failed)
    AppDetailFeedback.IconSaveFailed -> context.getString(R.string.app_detail_icon_save_failed)
    AppDetailFeedback.DocumentPickerUnavailable -> context.getString(R.string.app_detail_document_picker_unavailable)
    AppDetailFeedback.SummaryCopied -> context.getString(R.string.app_detail_summary_copied)
    AppDetailFeedback.ShareUnavailable -> context.getString(R.string.app_detail_share_unavailable)
}

internal fun formatTimestamp(instant: Instant): String {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return dateFormat.format(Date.from(instant))
}

internal fun AppDetailState.Loaded.toSummaryText(context: Context): String = buildList {
    add(context.getString(R.string.app_detail_summary_header, appName, packageName.value))
    add(summaryLine(context, R.string.app_detail_version, versionSummary(context)))
    minSdkSummary(context)?.let { add(summaryLine(context, R.string.general_info_min_sdk, it)) }
    targetSdkSummary(context)?.let { add(summaryLine(context, R.string.general_info_target_sdk, it)) }
    add(summaryLine(context, R.string.general_info_install_source, sourceSummary(context)))
    add(summaryLine(context, R.string.app_detail_apk_size, apkSize.formatted()))
    totalSize?.let { add(summaryLine(context, R.string.app_detail_total_size, it.formatted())) }
    signingSummary(context)?.let { add(summaryLine(context, R.string.app_detail_summary_signing_label, it)) }
    add(summaryLine(context, R.string.app_detail_permissions_section, permissionsSummary(context)))
}.joinToString("\n")

private fun summaryLine(
    context: Context,
    labelRes: Int,
    value: String,
): String = context.getString(R.string.app_detail_summary_line, context.getString(labelRes), value)

private fun AppDetailState.Loaded.versionSummary(context: Context): String =
    versionName?.let { context.getString(R.string.app_detail_summary_version, it, versionCode) }
        ?: versionCode.toString()

private fun AppDetailState.Loaded.minSdkSummary(context: Context): String? = when {
    minSdkLabel != null -> minSdkLabel
    minSdkVersion != null -> context.getString(R.string.app_detail_sdk_version_no_label, minSdkVersion)
    else -> null
}

private fun AppDetailState.Loaded.targetSdkSummary(context: Context): String? = when {
    targetSdkLabel != null -> targetSdkLabel
    targetSdkVersion != null -> context.getString(R.string.app_detail_sdk_version_no_label, targetSdkVersion)
    else -> null
}

private fun AppDetailState.Loaded.sourceSummary(context: Context): String = when (AppSource.valueOf(source)) {
    AppSource.GooglePlay -> context.getString(R.string.general_info_install_source_google_play)
    AppSource.SystemPreinstalled -> context.getString(R.string.general_info_install_source_system)
    AppSource.Unknown -> context.getString(R.string.general_info_install_source_unknown)
}

private fun AppDetailState.Loaded.signingSummary(context: Context): String? {
    val cert = certificate ?: return null
    return when (cert.trustLevel) {
        CertificateTrustLevel.Debug -> context.getString(R.string.app_detail_certificate_debug)
        CertificateTrustLevel.Valid -> cert.signerDisplayName ?: context.getString(R.string.certificates_self_signed)
    }
}

private fun AppDetailState.Loaded.permissionsSummary(context: Context): String = grantedDangerousPermissionsCount?.let { granted ->
    context.getString(
        R.string.app_detail_summary_permissions_granted,
        permissionCountSummary(
            context = context,
            count = totalPermissionsCount,
            pluralsRes = R.plurals.app_detail_summary_permissions_requested,
        ),
        permissionCountSummary(
            context = context,
            count = dangerousPermissionsCount,
            pluralsRes = R.plurals.app_detail_summary_permissions_dangerous,
        ),
        permissionCountSummary(
            context = context,
            count = granted,
            pluralsRes = R.plurals.app_detail_summary_permissions_granted_count,
        ),
    )
} ?: context.getString(
    R.string.app_detail_summary_permissions,
    permissionCountSummary(
        context = context,
        count = totalPermissionsCount,
        pluralsRes = R.plurals.app_detail_summary_permissions_requested,
    ),
    permissionCountSummary(
        context = context,
        count = dangerousPermissionsCount,
        pluralsRes = R.plurals.app_detail_summary_permissions_dangerous,
    ),
)

private fun permissionCountSummary(
    context: Context,
    count: Int,
    @PluralsRes pluralsRes: Int,
): String = context.resources.getQuantityString(pluralsRes, count, count)
