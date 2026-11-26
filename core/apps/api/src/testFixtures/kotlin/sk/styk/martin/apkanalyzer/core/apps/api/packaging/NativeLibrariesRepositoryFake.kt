package sk.styk.martin.apkanalyzer.core.apps.api.packaging

import sk.styk.martin.apkanalyzer.core.common.model.AppReference

class NativeLibrariesRepositoryFake(private val libraries: NativeLibraries = NativeLibraries.Empty) : NativeLibrariesRepository {
    override suspend fun nativeLibraries(reference: AppReference): Result<NativeLibraries> = Result.success(libraries)
}
