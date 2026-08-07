package sk.styk.martin.apkanalyzer.core.appindex

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.appindex.model.AppIndexStatus

interface AppIndexRepository {
    fun index(): Flow<AppIndexStatus>
}
