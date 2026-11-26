package sk.styk.martin.apkanalyzer.core.apps.api.packaging

import sk.styk.martin.apkanalyzer.core.common.model.AppReference

interface NativeLibrariesRepository {
    suspend fun nativeLibraries(reference: AppReference): Result<NativeLibraries>
}
