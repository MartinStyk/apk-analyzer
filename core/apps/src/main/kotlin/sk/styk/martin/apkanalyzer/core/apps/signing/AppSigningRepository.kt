package sk.styk.martin.apkanalyzer.core.apps.signing

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface AppSigningRepository {
    fun signing(): Flow<Map<PackageName, AppSigning>>
}
