package sk.styk.martin.apkanalyzer.core.apps.api

import sk.styk.martin.apkanalyzer.core.apps.api.model.AppDetail
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

interface AppDetailRepository {
    suspend fun details(reference: AppReference): Result<AppDetail>
}

class UnparsableApkFileException(path: String) : Exception("Cannot parse APK file: $path")
