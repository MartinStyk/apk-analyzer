package sk.styk.martin.apkanalyzer.core.apps

import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

interface AppDetailRepository {
    suspend fun details(reference: AppReference): Result<AppDetail>
}
