package sk.styk.martin.apkanalyzer.core.apps.permissions

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

data class PermissionDetails(
    val groupName: String?,
    val protectionLevel: ProtectionLevel,
    val protectionFlags: Set<ProtectionFlag>,
    val description: String?,
    val declaringPackage: PackageName,
)
