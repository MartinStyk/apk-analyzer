package sk.styk.martin.apkanalyzer.feature.appdetail.tv

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailBadge
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailInsight
import java.time.Instant

@Immutable
internal sealed interface TvAppDetailState {
    data object Loading : TvAppDetailState

    data class Error(val canRetry: Boolean) : TvAppDetailState

    @Immutable
    data class Loaded(
        val appReference: AppReference,
        val analysisMode: AppDetail.AnalysisMode,
        val appName: String,
        val packageName: PackageName,
        val versionName: String?,
        val versionCode: Long,
        val targetSdkVersion: Int?,
        val targetSdkLabel: String?,
        val apkSize: AppSize,
        val totalSize: AppSize?,
        val lastUpdateTime: Instant?,
        val badges: List<AppDetailBadge>,
        val insights: List<AppDetailInsight>,
        val totalPermissionsCount: Int,
        val sensitivePermissionsCount: Int,
        val grantedSensitivePermissionsCount: Int?,
        val permissionLevelCounts: Map<ProtectionLevel?, Int>,
        val componentCounts: Map<ComponentKind, Int>,
        val signerName: String?,
        val isDebugSigned: Boolean,
        val sha256Fingerprint: String?,
        val requiredFeaturesCount: Int,
        val optionalFeaturesCount: Int,
        val missingRequiredFeaturesCount: Int,
    ) : TvAppDetailState {
        val isTargetSdkOutdated: Boolean
            get() = insights.any { it is AppDetailInsight.OutdatedTargetSdk }
    }
}
