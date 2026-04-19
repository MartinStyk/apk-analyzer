package sk.styk.martin.apkanalyzer.core.appanalysis

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import sk.styk.martin.apkanalyzer.core.appanalysis.model.PermissionData
import sk.styk.martin.apkanalyzer.core.appanalysis.model.PermissionDataAggregate
import sk.styk.martin.apkanalyzer.core.appanalysis.model.UsedPermissionData
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import javax.inject.Inject

class AppPermissionManager
@Inject
internal constructor(private val packageManager: PackageManager) {
    fun getPermissions(packageInfo: PackageInfo): PermissionDataAggregate {
        val definedPermissions = getDefinedPermissions(packageInfo)
        val requestedPermissions = getUsedPermissions(packageInfo)

        return PermissionDataAggregate(definedPermissions, requestedPermissions)
    }

    fun getUsedPermissions(packageName: String): List<UsedPermissionData>? = try {
        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        getUsedPermissions(packageInfo)
    } catch (e: Exception) {
        Logger.w(TAG_APP_ANALYSIS, e, "Get permission detail failed")
        null
    }

    private fun getDefinedPermissions(packageInfo: PackageInfo): List<PermissionData> = packageInfo.permissions.orEmpty().map {
        PermissionData(
            name = it.name,
            simpleName = createSimpleName(it.name),
            groupName = it.group,
            protectionLevel = it.protectionLevel,
        )
    }

    private fun getUsedPermissions(packageInfo: PackageInfo): List<UsedPermissionData> {
        val requestedPermissionNames = packageInfo.requestedPermissions.orEmpty()
        val requestedPermissionFlags = packageInfo.requestedPermissionsFlags
        val requestedPermissions: MutableList<UsedPermissionData> = ArrayList(requestedPermissionNames.size)

        requestedPermissionNames.forEachIndexed { index, name ->
            val isGranted = ((requestedPermissionFlags?.getOrNull(index) ?: 0) and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0)

            val permissionData =
                try {
                    val permissionInfo = packageManager.getPermissionInfo(name, PackageManager.GET_META_DATA)
                    PermissionData(
                        name = name,
                        simpleName = createSimpleName(name),
                        groupName = permissionInfo.group,
                        protectionLevel = permissionInfo.protectionLevel,
                    )
                } catch (e: Exception) {
                    // we failed to get permission data from package manager. Try to use things we know
                    PermissionData(
                        name = name,
                        simpleName = createSimpleName(name),
                    )
                }

            requestedPermissions.add(UsedPermissionData(permissionData, isGranted))
        }
        return requestedPermissions
    }

    private fun createSimpleName(name: String): String {
        var simpleNameBuilder = StringBuilder(name)

        val lastDot = name.lastIndexOf(".")
        if (lastDot > 0 && lastDot < name.length) {
            simpleNameBuilder = StringBuilder(name.substring(lastDot + 1))
        }

        var i = 0
        var previousSpace = false
        while (++i < simpleNameBuilder.length) {
            if (simpleNameBuilder[i] == '_') {
                simpleNameBuilder.replace(i, i + 1, " ")
                previousSpace = true
            } else {
                if (!previousSpace) {
                    val lowercase = Character.toLowerCase(simpleNameBuilder[i])
                    simpleNameBuilder.replace(i, i + 1, lowercase.toString())
                }
                previousSpace = false
            }
        }
        return simpleNameBuilder.toString()
    }
}
