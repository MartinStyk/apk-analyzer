package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import android.content.Context
import androidx.annotation.PluralsRes
import dagger.hilt.android.qualifiers.ApplicationContext
import sk.styk.martin.apkanalyzer.core.apps.model.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import javax.inject.Inject

internal class AppSummaryTextFormatterImpl @Inject constructor(@ApplicationContext private val context: Context) : AppSummaryTextFormatter {

    override fun summary(state: AppDetailState.Loaded): String = with(state) {
        buildList {
            add(context.getString(R.string.app_detail_summary_header, appName, packageName.value))
            add(summaryLine(R.string.app_detail_version, versionSummary()))
            minSdkSummary()?.let { add(summaryLine(R.string.general_info_min_sdk, it)) }
            targetSdkSummary()?.let { add(summaryLine(R.string.general_info_target_sdk, it)) }
            add(summaryLine(R.string.general_info_install_source, sourceSummary()))
            add(summaryLine(R.string.app_detail_apk_size, apkSize.formatted()))
            totalSize?.let { add(summaryLine(R.string.app_detail_total_size, it.formatted())) }
            signingSummary()?.let { add(summaryLine(R.string.app_detail_summary_signing_label, it)) }
            add(summaryLine(R.string.app_detail_permissions_section, permissionsSummary()))
        }.joinToString("\n")
    }

    override fun clipLabel(appName: String): String = context.getString(R.string.app_detail_summary_clip_label, appName)

    private fun summaryLine(labelRes: Int, value: String): String = context.getString(R.string.app_detail_summary_line, context.getString(labelRes), value)

    private fun AppDetailState.Loaded.versionSummary(): String = versionName?.let { context.getString(R.string.app_detail_summary_version, it, versionCode) }
        ?: versionCode.toString()

    private fun AppDetailState.Loaded.minSdkSummary(): String? = when {
        minSdkLabel != null -> minSdkLabel
        minSdkVersion != null -> context.getString(R.string.app_detail_sdk_version_no_label, minSdkVersion)
        else -> null
    }

    private fun AppDetailState.Loaded.targetSdkSummary(): String? = when {
        targetSdkLabel != null -> targetSdkLabel
        targetSdkVersion != null -> context.getString(R.string.app_detail_sdk_version_no_label, targetSdkVersion)
        else -> null
    }

    private fun AppDetailState.Loaded.sourceSummary(): String = when (source) {
        AppSource.GooglePlay -> context.getString(R.string.general_info_install_source_google_play)
        AppSource.SystemPreinstalled -> context.getString(R.string.general_info_install_source_system)
        AppSource.Unknown -> context.getString(R.string.general_info_install_source_unknown)
    }

    private fun AppDetailState.Loaded.signingSummary(): String? {
        val cert = certificate ?: return null
        return when (cert.trustLevel) {
            CertificateTrustLevel.Debug -> context.getString(R.string.app_detail_certificate_debug)
            CertificateTrustLevel.Valid -> cert.signerDisplayName ?: context.getString(R.string.certificates_self_signed)
        }
    }

    private fun AppDetailState.Loaded.permissionsSummary(): String = grantedDangerousPermissionsCount?.let { granted ->
        context.getString(
            R.string.app_detail_summary_permissions_granted,
            permissionCountSummary(totalPermissionsCount, R.plurals.app_detail_summary_permissions_requested),
            permissionCountSummary(dangerousPermissionsCount, R.plurals.app_detail_summary_permissions_dangerous),
            permissionCountSummary(granted, R.plurals.app_detail_summary_permissions_granted_count),
        )
    } ?: context.getString(
        R.string.app_detail_summary_permissions,
        permissionCountSummary(totalPermissionsCount, R.plurals.app_detail_summary_permissions_requested),
        permissionCountSummary(dangerousPermissionsCount, R.plurals.app_detail_summary_permissions_dangerous),
    )

    private fun permissionCountSummary(count: Int, @PluralsRes pluralsRes: Int): String = context.resources.getQuantityString(pluralsRes, count, count)
}
