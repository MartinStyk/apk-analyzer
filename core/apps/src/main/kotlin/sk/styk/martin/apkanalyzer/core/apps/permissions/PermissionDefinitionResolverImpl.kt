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

@Suppress("DEPRECATION")
private fun resolveProtectionLevel(protection: Int): ProtectionLevel = when (protection) {
    PermissionInfo.PROTECTION_DANGEROUS -> ProtectionLevel.Dangerous
    PermissionInfo.PROTECTION_SIGNATURE, PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM -> ProtectionLevel.Signature
    PermissionInfo.PROTECTION_INTERNAL -> ProtectionLevel.Internal
    else -> ProtectionLevel.Normal
}

private fun resolveProtectionFlags(protectionFlags: Int): Set<ProtectionFlag> = mapOf(
    PermissionInfo.PROTECTION_FLAG_PRIVILEGED to ProtectionFlag.Privileged,
    PermissionInfo.PROTECTION_FLAG_APPOP to ProtectionFlag.AppOp,
    PermissionInfo.PROTECTION_FLAG_INSTANT to ProtectionFlag.Instant,
    PermissionInfo.PROTECTION_FLAG_DEVELOPMENT to ProtectionFlag.Development,
).filterKeys { protectionFlags and it != 0 }
    .values
    .toSet()
