package sk.styk.martin.apkanalyzer.core.apps.signing

import sk.styk.martin.apkanalyzer.core.common.model.AppReference

interface SigningSchemeRepository {
    suspend fun signingSchemeVersions(reference: AppReference): Result<List<SigningSchemeVersion>?>
}
