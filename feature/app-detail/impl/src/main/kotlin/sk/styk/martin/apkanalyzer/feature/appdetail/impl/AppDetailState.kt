package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.AppDetailBadge
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.insight.AppDetailInsight
import java.time.Instant

internal enum class AppDetailExport {
    Apk,
    Icon,
}

internal enum class OverviewHighlight {
    None,
    InstallDate,
    InstallHistory,
    NetworkSecurity,
    SplitApks,
}

@Immutable
internal sealed interface AppDetailState {
    data object Loading : AppDetailState

    @Immutable
    data class Loaded(
        val analysisMode: AppDetail.AnalysisMode,
        val appName: String,
        val packageName: PackageName,
        val processName: String?,
        val versionName: String?,
        val versionCode: Long,
        val uid: Int?,
        val description: String?,
        val isSystemApp: Boolean,
        val source: AppSource,
        val apkDirectory: String?,
        val dataDirectory: String?,
        val apkSize: AppSize,
        val totalSize: AppSize? = null,
        val targetSdkVersion: Int?,
        val targetSdkLabel: String?,
        val minSdkVersion: Int?,
        val minSdkLabel: String?,
        val installLocation: String,
        val appInstaller: PackageName?,
        val firstInstallTime: Instant?,
        val lastUpdateTime: Instant?,
        val lastUsedTime: Instant? = null,
        val installedSplitsCount: Int = 0,
        val usesCleartextTraffic: Boolean = false,
        val totalPermissionsCount: Int,
        val dangerousPermissionsCount: Int,
        val grantedDangerousPermissionsCount: Int?,
        val dangerousPermissionPreviews: ImmutableList<PermissionPreview>,
        val definedPermissionsCount: Int,
        val activitiesCount: Int,
        val servicesCount: Int,
        val contentProvidersCount: Int,
        val broadcastReceiversCount: Int,
        val certificatesCount: Int,
        val requirementsCount: Int,
        val requiredFeaturesCount: Int,
        val optionalFeaturesCount: Int,
        val unmetRequirementsCount: Int,
        val requirementPreviews: ImmutableList<RequirementPreview>,
        val certificate: CertificateState? = null,
        val badges: ImmutableList<AppDetailBadge> = persistentListOf(),
        val insights: ImmutableList<AppDetailInsight> = persistentListOf(),
        val exportInProgress: AppDetailExport? = null,
    ) : AppDetailState {
        val isTargetSdkOutdated: Boolean
            get() = insights.any { it is AppDetailInsight.OutdatedTargetSdk }

        val additionalGeneralInfoCount: Int
            get() {
                val optionalDetailsCount = listOfNotNull(
                    processName,
                    uid,
                    description,
                    appInstaller,
                    firstInstallTime,
                    lastUsedTime,
                    apkDirectory,
                    dataDirectory,
                    minSdkVersion,
                ).size
                val installLocationRow = 1
                val securitySectionRows = 2
                val nativeLibrarySectionRows = 2
                val splitApksRow = if (installedSplitsCount > 0) 1 else 0
                return optionalDetailsCount + installLocationRow + securitySectionRows + nativeLibrarySectionRows + splitApksRow
            }

        val overviewHighlight: OverviewHighlight
            get() = when {
                installedSplitsCount > 0 -> OverviewHighlight.SplitApks
                usesCleartextTraffic -> OverviewHighlight.NetworkSecurity
                firstInstallTime != null && lastUsedTime != null -> OverviewHighlight.InstallHistory
                firstInstallTime != null || lastUsedTime != null -> OverviewHighlight.InstallDate
                else -> OverviewHighlight.None
            }

        @Immutable
        data class CertificateState(
            val signAlgorithm: String,
            val sha256Fingerprint: String,
            val issuer: CertificatePrincipal,
            val trustLevel: CertificateTrustLevel,
        ) {
            val signerDisplayName: String?
                get() = issuer.displayName
        }

        @Immutable
        data class PermissionPreview(
            val name: String,
            val groupName: String?,
            val label: String,
        )

        @Immutable
        data class RequirementPreview(val name: String?, val isUnmetRequirement: Boolean)
    }

    @Immutable
    data class Error(val canRetry: Boolean) : AppDetailState
}
