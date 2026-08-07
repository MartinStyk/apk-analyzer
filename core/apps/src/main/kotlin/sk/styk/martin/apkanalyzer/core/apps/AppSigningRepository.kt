package sk.styk.martin.apkanalyzer.core.apps

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apps.model.AppSigning
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface AppSigningRepository {
    fun signing(): Flow<Map<PackageName, AppSigning>>
}
