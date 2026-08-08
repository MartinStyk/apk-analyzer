package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import android.Manifest
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import sk.styk.martin.apkanalyzer.core.apps.AppClassificationThresholds
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionLevel
import java.time.Instant

internal object AppDetailInsightEvaluator {

    fun evaluate(
        appDetail: AppDetail,
        now: Instant,
        deviceSdk: Int,
    ): ImmutableList<AppDetailInsight> = with(appDetail) {
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
            val targetSdk = info.targetSdkVersion
            if (!info.isSystemApp && targetSdk != null && deviceSdk - targetSdk >= AppClassificationThresholds.OUTDATED_TARGET_SDK_RELEASE_GAP) {
                add(
                    AppDetailInsight.OutdatedTargetSdk(
                        targetSdk = targetSdk,
                        deviceSdk = deviceSdk,
                    ),
                )
            }
            if (analysisMode == AppDetail.AnalysisMode.InstalledPackage) {
                val grantedDangerousPermissionNames = permissions.used
                    .filter {
                        it.isGranted &&
                            it.permissionData.details?.protectionLevel == ProtectionLevel.Dangerous
                    }
                    .map { it.permissionData.name }
                    .toSet()
                sensitivePermissionGroups.forEach { (access, names) ->
                    grantedDangerousPermissionNames.firstOrNull { it in names }?.let { permissionName ->
                        add(AppDetailInsight.SensitivePermission(access, permissionName))
                    }
                }
            }
        }.toImmutableList()
    }
}

@Suppress("DEPRECATION")
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
