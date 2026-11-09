package sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import sk.styk.martin.apkanalyzer.core.apps.AppClassificationThresholds
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.common.model.isSideloaded
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.isOnDevice
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.recordedAt
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class AppDetailInsightEvaluator @Inject constructor() {

    fun evaluate(appDetail: AppDetail, input: AppDetailInput): List<AppDetailInsight> = insights(
        appDetail = appDetail,
        now = input.recordedAt ?: Instant.now(),
        deviceSdk = Build.VERSION.SDK_INT.takeIf { input.isOnDevice },
    )

    private fun insights(
        appDetail: AppDetail,
        now: Instant,
        deviceSdk: Int?,
    ): List<AppDetailInsight> = with(appDetail) {
        buildList {
            if (info.isDebuggable) {
                add(AppDetailInsight.Debuggable)
            }
            val currentCertificates = signing.currentCertificates
            if (currentCertificates.any { it.trustLevel == CertificateTrustLevel.Debug }) {
                add(AppDetailInsight.DebugCertificate)
            }
            if (currentCertificates.any { now.isBefore(it.validFrom) }) {
                add(AppDetailInsight.CertificateNotYetValid)
            }
            if (analysisMode == AppDetail.AnalysisMode.InstalledPackage && info.source.isSideloaded) {
                add(AppDetailInsight.Sideloaded)
            }
            outdatedTargetSdk(deviceSdk)?.let { add(it) }
            if (analysisMode == AppDetail.AnalysisMode.InstalledPackage) {
                info.lastUsedTime?.let { lastUsed ->
                    if (lastUsed.isBefore(now.minus(AppClassificationThresholds.UNUSED_PERIOD))) {
                        add(AppDetailInsight.Unused(monthsSinceLastUsed = monthsBetween(lastUsed, now)))
                    }
                }
                val grantedDangerousPermissionNames = permissions.used
                    .filter { it.isGranted && it.isDangerous }
                    .map { it.permissionData.name }
                    .toSet()
                sensitivePermissionGroups.forEach { (access, names) ->
                    grantedDangerousPermissionNames.firstOrNull { it in names }?.let { permissionName ->
                        add(AppDetailInsight.SensitivePermission(access, permissionName))
                    }
                }
            }
        }
    }
}

private fun AppDetail.outdatedTargetSdk(deviceSdk: Int?): AppDetailInsight.OutdatedTargetSdk? {
    val targetSdk = info.targetSdkVersion
    if (deviceSdk == null || targetSdk == null || info.isSystemApp) return null
    return AppDetailInsight.OutdatedTargetSdk(targetSdk = targetSdk, deviceSdk = deviceSdk)
        .takeIf { it.apiLevelGap >= AppClassificationThresholds.OUTDATED_TARGET_SDK_RELEASE_GAP }
}

private const val DAYS_PER_MONTH = 30

private fun monthsBetween(from: Instant, to: Instant): Int {
    val months = Duration.between(from, to).toDays() / DAYS_PER_MONTH
    return months.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
}

@Suppress("DEPRECATION")
@SuppressLint("InlinedApi")
private val sensitivePermissionGroups = linkedMapOf(
    SensitiveAccess.BackgroundLocation to setOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
    SensitiveAccess.Messages to setOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
    ),
    SensitiveAccess.CallHistory to setOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.PROCESS_OUTGOING_CALLS,
    ),
    SensitiveAccess.Contacts to setOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
    ),
    SensitiveAccess.Calendar to setOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR,
    ),
)
