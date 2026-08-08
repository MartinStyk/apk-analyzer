package sk.styk.martin.apkanalyzer.core.apppermissions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import sk.styk.martin.apkanalyzer.core.apppermissions.model.DevicePermission
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DevicePermissionsRepositoryImpl @Inject constructor(
    installedAppsRepository: InstalledAppsRepository,
    permissionLabelProvider: PermissionLabelProvider,
    dispatcherProvider: DispatcherProvider,
    appScope: CoroutineScope,
) : DevicePermissionsRepository {

    private val cachedPermissions = installedAppsRepository.apps()
        .map { apps ->
            apps
                .flatMap { it.requestedPermissions }
                .distinct()
                .sorted()
                .map { name -> DevicePermission(name = name, label = permissionLabelProvider.getLabel(name)) }
        }
        .flowOn(dispatcherProvider.io())
        .shareIn(appScope, SharingStarted.Lazily, replay = 1)

    override fun permissions(): Flow<List<DevicePermission>> = cachedPermissions
}
