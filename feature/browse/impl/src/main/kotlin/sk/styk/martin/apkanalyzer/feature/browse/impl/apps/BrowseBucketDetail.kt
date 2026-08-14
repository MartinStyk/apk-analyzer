package sk.styk.martin.apkanalyzer.feature.browse.impl.apps

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.signing.SignatureAlgorithmAssessment

@Immutable
internal sealed interface BrowseBucketDetail {

    @Immutable
    data class Permission(val protectionLevel: ProtectionLevel?, val description: String?) : BrowseBucketDetail

    @Immutable
    data class Certificate(
        val algorithm: String,
        val algorithmAssessment: SignatureAlgorithmAssessment,
        val isSelfSigned: Boolean,
    ) : BrowseBucketDetail
}
