package sk.styk.martin.apkanalyzer.core.apppermissions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import sk.styk.martin.apkanalyzer.core.appanalysis.AppPermissionManager
import sk.styk.martin.apkanalyzer.core.appanalysis.model.PermissionData
import sk.styk.martin.apkanalyzer.core.appanalysis.model.UsedPermissionData
import sk.styk.martin.apkanalyzer.core.applist.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.applist.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apppermissions.model.LocalPermissionData
import sk.styk.martin.apkanalyzer.core.apppermissions.model.LocalPermissionStatus
import java.util.TreeSet
import javax.inject.Inject

class LocalPermissionManager
@Inject
constructor(private val appPermissionManager: AppPermissionManager, private val installedAppsRepository: InstalledAppsRepository) {
    sealed class PermissionLoadingStatus {
        data class Loading(val currentProgress: Int, val totalProgress: Int) : PermissionLoadingStatus()

        data class Data(val localPermissionData: List<LocalPermissionData>) : PermissionLoadingStatus()
    }

    fun observeAllPermissionData(): Flow<PermissionLoadingStatus> = installedAppsRepository.apps().flatMapConcat { allApps ->
        flow {
            emit(PermissionLoadingStatus.Loading(0, allApps.size))

            val builder = LocalPermissionDataBuilder()

            allApps.forEachIndexed { index, appListData ->
                builder += appListData
                emit(PermissionLoadingStatus.Loading(index, allApps.size))
            }

            emit(PermissionLoadingStatus.Data(builder.build()))
        }
    }

    private inner class LocalPermissionDataBuilder {
        private val data = HashMap<PermissionData, MutableList<LocalPermissionStatus>>()

        operator fun plusAssign(installedApp: InstalledApp) {
            val packageName = installedApp.packageName
            appPermissionManager.getUsedPermissions(packageName)?.forEach { permissionData ->
                add(packageName, permissionData)
            }
        }

        private fun add(packageName: String, usedPermissionData: UsedPermissionData) {
            val packageNamesForGivenPermissions = data[usedPermissionData.permissionData] ?: mutableListOf()
            packageNamesForGivenPermissions.add(LocalPermissionStatus(packageName, usedPermissionData.isGranted))
            data[usedPermissionData.permissionData] = packageNamesForGivenPermissions
        }

        fun build(): List<LocalPermissionData> {
            val sortedSet = TreeSet<Map.Entry<PermissionData, List<LocalPermissionStatus>>> { entry1, entry2 -> entry2.value.size - entry1.value.size }
            sortedSet.addAll(data.entries)

            return sortedSet.mapTo(ArrayList(sortedSet.size)) { LocalPermissionData(it.key, it.value) }
        }
    }
}
