package sk.styk.martin.apkanalyzer.core.apphistory.capture.snapshot

import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.ProviderPathPermission

@Serializable
internal data class ContentProviderSnapshot(
    val name: String,
    val authority: String?,
    val readPermission: String?,
    val writePermission: String?,
    val isExported: Boolean,
    val pathPermissions: List<ProviderPathPermissionSnapshot>,
)

@Serializable
internal data class ProviderPathPermissionSnapshot(
    val path: String,
    val matchType: String,
    val readPermission: String?,
    val writePermission: String?,
)

internal fun ContentProvider.toSnapshot() = ContentProviderSnapshot(
    name = name,
    authority = authority,
    readPermission = readPermission,
    writePermission = writePermission,
    isExported = isExported,
    pathPermissions = pathPermissions.map { it.toSnapshot() }.sortedWith(compareBy({ it.path }, { it.matchType })),
)

internal fun ProviderPathPermission.toSnapshot() = ProviderPathPermissionSnapshot(
    path = path,
    matchType = matchType.name,
    readPermission = readPermission,
    writePermission = writePermission,
)
