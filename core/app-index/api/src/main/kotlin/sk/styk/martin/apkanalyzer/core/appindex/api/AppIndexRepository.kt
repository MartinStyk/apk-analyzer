package sk.styk.martin.apkanalyzer.core.appindex.api

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.appindex.api.model.AppIndexStatus

interface AppIndexRepository {
    fun index(): Flow<AppIndexStatus>
}
