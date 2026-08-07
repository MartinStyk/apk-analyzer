package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.model.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.AppDetailBadge
import java.time.Instant

internal enum class AppDetailExport {
    Apk,
    Icon,
}

@Immutable
internal sealed interface AppDetailState {
    data object Loading : AppDetailState

    @Immutable
    data class Loaded(
        val analysisMode: AppDetail.AnalysisMode,
        val appName: String,
        val packageName: String,
        val processName: String?,
        val versionName: String?,
        val versionCode: Long,
        val uid: Int?,
        val description: String?,
        val isSystemApp: Boolean,
        val source: String,
        val apkDirectory: String?,
        val dataDirectory: String?,
        val apkSize: AppSize,
        val totalSize: AppSize? = null,
        val targetSdkVersion: Int?,
        val targetSdkLabel: String?,
        val minSdkVersion: Int?,
        val minSdkLabel: String?,
        val installLocation: String,
        val appInstaller: String?,
        val firstInstallTime: Instant?,
        val lastUpdateTime: Instant?,
        val lastUsedTime: Instant? = null,
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

        @Immutable
        data class CertificateState(
            val signAlgorithm: String,
            val sha256Fingerprint: String,
            val issuer: CertificatePrincipal,
            val trustLevel: CertificateTrustLevel,
            val validFrom: Instant,
            val validUntil: Instant,
            val isSelfSigned: Boolean,
        ) {
            val signerDisplayName: String?
                get() = issuer.displayName

            fun validity(now: Instant): CertificateValidity = when {
                now.isBefore(validFrom) -> CertificateValidity.NotYetValid
                now.isAfter(validUntil) -> CertificateValidity.Expired
                else -> CertificateValidity.Valid
            }
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

    data object Error : AppDetailState
}

internal enum class CertificateValidity {
    Valid,
    Expired,
    NotYetValid,
}
