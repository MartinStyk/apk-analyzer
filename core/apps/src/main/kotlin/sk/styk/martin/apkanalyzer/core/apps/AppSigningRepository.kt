package sk.styk.martin.apkanalyzer.core.apps

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apps.model.AppSigning

interface AppSigningRepository {
    fun signing(): Flow<Map<String, AppSigning>>
}
