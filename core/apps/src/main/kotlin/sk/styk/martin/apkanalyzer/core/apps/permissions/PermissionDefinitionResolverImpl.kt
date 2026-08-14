package sk.styk.martin.apkanalyzer.core.apps.permissions

import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PermissionDefinitionResolverImpl @Inject constructor(private val packageManager: PackageManager) : PermissionDefinitionResolver {

    override fun resolve(name: String): PermissionDetails? = try {
        packageManager.getPermissionInfo(name, PackageManager.GET_META_DATA).toPermissionDetails(packageManager)
    } catch (_: Exception) {
        null
    }
}

fun PermissionInfo.toPermissionDetails(packageManager: PackageManager) = PermissionDetails(
    groupName = group,
    protectionLevel = resolveProtectionLevel(protection),
    protectionFlags = resolveProtectionFlags(protectionFlags),
    description = loadDescription(packageManager)?.toString(),
    declaringPackage = PackageName(packageName),
)
