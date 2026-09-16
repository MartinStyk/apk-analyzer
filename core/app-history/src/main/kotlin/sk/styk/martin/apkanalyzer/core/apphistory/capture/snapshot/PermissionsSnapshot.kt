package sk.styk.martin.apkanalyzer.core.apphistory.capture.snapshot

import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.permissions.PermissionDetails
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permissions

@Serializable
internal data class PermissionsSnapshot(val defined: List<PermissionSnapshot>, val used: List<PermissionSnapshot>)

@Serializable
internal data class PermissionSnapshot(val name: String, val details: PermissionDetailsSnapshot?)

@Serializable
internal data class PermissionDetailsSnapshot(
    val groupName: String?,
    val protectionLevel: String,
    val protectionFlags: Set<String>,
    val description: String?,
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
    description = description,
    declaringPackage = declaringPackage.value,
)
