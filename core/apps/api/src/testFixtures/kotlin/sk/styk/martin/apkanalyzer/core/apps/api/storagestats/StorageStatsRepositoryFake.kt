package sk.styk.martin.apkanalyzer.core.apps.storagestats

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

class StorageStatsRepositoryFake(totalSizes: Map<PackageName, AppSize> = emptyMap()) : StorageStatsRepository {
    override val isPermissionGranted: StateFlow<Boolean> = MutableStateFlow(true)
    override val totalSizes: StateFlow<Map<PackageName, AppSize>> = MutableStateFlow(totalSizes)

    override fun requestTotalSizes(packageNames: List<PackageName>) = Unit

    override suspend fun queryTotalSize(packageName: PackageName): AppSize? = totalSizes.value[packageName]

    override suspend fun ensureLoaded(packageNames: List<PackageName>) = Unit
}
