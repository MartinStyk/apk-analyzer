package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import android.content.Context
import sk.styk.martin.apkanalyzer.core.apps.model.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.resources.ResourcesManager
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

internal fun AppDetailState.Loaded.toSummaryText(resourcesManager: ResourcesManager): String = buildList {
    add(resourcesManager.getString(R.string.app_detail_summary_header, appName, packageName.value).toString())
    add(summaryLine(resourcesManager, R.string.app_detail_version, versionSummary(resourcesManager)))
    minSdkSummary(resourcesManager)?.let { add(summaryLine(resourcesManager, R.string.general_info_min_sdk, it)) }
    targetSdkSummary(resourcesManager)?.let { add(summaryLine(resourcesManager, R.string.general_info_target_sdk, it)) }
    add(summaryLine(resourcesManager, R.string.general_info_install_source, sourceSummary(resourcesManager)))
    add(summaryLine(resourcesManager, R.string.app_detail_apk_size, apkSize.formatted()))
    totalSize?.let { add(summaryLine(resourcesManager, R.string.app_detail_total_size, it.formatted())) }
    signingSummary(resourcesManager)?.let { add(summaryLine(resourcesManager, R.string.app_detail_summary_signing_label, it)) }
    add(summaryLine(resourcesManager, R.string.app_detail_permissions_section, permissionsSummary(resourcesManager)))
}.joinToString("\n")

private fun summaryLine(
    resourcesManager: ResourcesManager,
    labelRes: Int,
    value: String,
): String = resourcesManager.getString(R.string.app_detail_summary_line, resourcesManager.getString(labelRes), value).toString()

private fun AppDetailState.Loaded.versionSummary(resourcesManager: ResourcesManager): String =
    versionName?.let { resourcesManager.getString(R.string.app_detail_summary_version, it, versionCode).toString() }
        ?: versionCode.toString()

private fun AppDetailState.Loaded.minSdkSummary(resourcesManager: ResourcesManager): String? = when {
    minSdkLabel != null -> minSdkLabel
    minSdkVersion != null -> resourcesManager.getString(R.string.app_detail_sdk_version_no_label, minSdkVersion).toString()
    else -> null
}

private fun AppDetailState.Loaded.targetSdkSummary(resourcesManager: ResourcesManager): String? = when {
    targetSdkLabel != null -> targetSdkLabel
    targetSdkVersion != null -> resourcesManager.getString(R.string.app_detail_sdk_version_no_label, targetSdkVersion).toString()
    else -> null
}

private fun AppDetailState.Loaded.sourceSummary(resourcesManager: ResourcesManager): String = when (AppSource.valueOf(source)) {
    AppSource.GooglePlay -> resourcesManager.getString(R.string.general_info_install_source_google_play)
    AppSource.SystemPreinstalled -> resourcesManager.getString(R.string.general_info_install_source_system)
    AppSource.Unknown -> resourcesManager.getString(R.string.general_info_install_source_unknown)
}.toString()

private fun AppDetailState.Loaded.signingSummary(resourcesManager: ResourcesManager): String? {
    val cert = certificate ?: return null
    return when (cert.trustLevel) {
        CertificateTrustLevel.Debug -> resourcesManager.getString(R.string.app_detail_certificate_debug).toString()
        CertificateTrustLevel.Valid -> cert.signerDisplayName ?: resourcesManager.getString(R.string.certificates_self_signed).toString()
    }
}

private fun AppDetailState.Loaded.permissionsSummary(resourcesManager: ResourcesManager): String = grantedDangerousPermissionsCount?.let { granted ->
    resourcesManager.getString(
        R.string.app_detail_summary_permissions_granted,
        totalPermissionsCount,
        dangerousPermissionsCount,
        granted,
    ).toString()
} ?: resourcesManager.getString(
    R.string.app_detail_summary_permissions,
    totalPermissionsCount,
    dangerousPermissionsCount,
).toString()
