package sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot

import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.permissions.PermissionDetails
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permissions
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.permissions.UsedPermission
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

@Serializable
internal data class PermissionsSnapshot(val defined: List<PermissionSnapshot>, val used: List<PermissionSnapshot>)

@Serializable
internal data class PermissionSnapshot(val name: String, val details: PermissionDetailsSnapshot?)

@Serializable
internal data class PermissionDetailsSnapshot(
    val groupName: String?,
    val protectionLevel: String,
    val protectionFlags: Set<String>,
    val declaringPackage: String,
)

internal fun Permissions.toSnapshot() = PermissionsSnapshot(
    defined = defined.map { it.toSnapshot() }.sortedBy { it.name },
    used = used.map { it.permissionData.toSnapshot() }.sortedBy { it.name },
)

internal fun Permission.toSnapshot() = PermissionSnapshot(
    name = name,
    details = details?.toSnapshot(),
)

internal fun PermissionDetails.toSnapshot() = PermissionDetailsSnapshot(
    groupName = groupName,
    protectionLevel = protectionLevel.name,
    protectionFlags = protectionFlags.map { it.name }.sorted().toSet(),
    declaringPackage = declaringPackage.value,
)

internal fun PermissionsSnapshot.toDomain() = Permissions(
    defined = defined.map { it.toDomain() },
    used = used.map { UsedPermission(permissionData = it.toDomain(), isGranted = false) },
)

internal fun PermissionSnapshot.toDomain() = Permission(
    name = name,
    details = details?.toDomain(),
)

internal fun PermissionDetailsSnapshot.toDomain() = PermissionDetails(
    groupName = groupName,
    protectionLevel = ProtectionLevel.valueOf(protectionLevel),
    protectionFlags = protectionFlags.map { ProtectionFlag.valueOf(it) }.toSet(),
    description = null,
    declaringPackage = PackageName(declaringPackage),
)
