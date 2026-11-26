package sk.styk.martin.apkanalyzer.core.apps.api.signing

import sk.styk.martin.apkanalyzer.core.common.model.AppReference

interface SigningSchemeRepository {
    suspend fun signingSchemeVersions(reference: AppReference): Result<List<SigningSchemeVersion>?>
}
