package sk.styk.martin.apkanalyzer.core.appindex.impl.browse

import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseBucketDetail
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseBucketDetailResolver
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseSubAttribute
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.isCertificateHash
import sk.styk.martin.apkanalyzer.core.appindex.api.model.AppAttributeIndex
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.PermissionDefinitionResolver
import sk.styk.martin.apkanalyzer.core.apps.api.signing.validityAt
import java.time.Instant
import javax.inject.Inject

internal class BrowseBucketDetailResolverImpl @Inject constructor(private val permissionDefinitionResolver: PermissionDefinitionResolver) :
    BrowseBucketDetailResolver {

    override fun resolve(
        index: AppAttributeIndex,
        dimension: BrowseDimension,
        bucketKey: String,
        subAttribute: BrowseSubAttribute?,
    ): BrowseBucketDetail? = when (dimension) {
        BrowseDimension.Permission -> permissionDefinitionResolver.resolve(bucketKey)?.let {
            BrowseBucketDetail.Permission(protectionLevel = it.protectionLevel, description = it.description)
        }

        BrowseDimension.SigningCertificate -> subAttribute.takeIf { it.isCertificateHash }
            ?.let { index.certificateByHash[bucketKey] }
            ?.let {
                BrowseBucketDetail.Certificate(
                    algorithm = it.signAlgorithm,
                    algorithmAssessment = it.signatureAlgorithmAssessment,
                    isSelfSigned = it.isSelfSigned,
                    subject = it.subject,
                    issuer = it.issuer,
                    validFrom = it.validFrom,
                    validUntil = it.validUntil,
                    validity = it.validityAt(Instant.now()),
                )
            }

        BrowseDimension.TargetSdk,
        BrowseDimension.MinSdk,
        BrowseDimension.InstallSource,
        BrowseDimension.SharedUserId,
        BrowseDimension.AppCategory,
        -> null
    }
}
