package sk.styk.martin.apkanalyzer.core.apps.api.signing

import sk.styk.martin.apkanalyzer.core.common.model.AppReference

class SigningSchemeRepositoryFake(private val versions: List<SigningSchemeVersion>? = emptyList()) : SigningSchemeRepository {
    override suspend fun signingSchemeVersions(reference: AppReference): Result<List<SigningSchemeVersion>?> = Result.success(versions)
}
