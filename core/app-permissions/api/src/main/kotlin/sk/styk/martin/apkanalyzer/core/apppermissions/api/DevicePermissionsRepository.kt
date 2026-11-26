package sk.styk.martin.apkanalyzer.core.apppermissions.api

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apppermissions.api.model.DevicePermission

interface DevicePermissionsRepository {
    fun permissions(): Flow<List<DevicePermission>>
}
