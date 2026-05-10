package sk.styk.martin.apkanalyzer.core.apppermissions

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apppermissions.model.DevicePermission

interface DevicePermissionsRepository {
    fun permissions(): Flow<List<DevicePermission>>
}
