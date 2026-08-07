package sk.styk.martin.apkanalyzer.core.apps.model

data class PermissionDetails(
    val groupName: String?,
    val protectionLevel: ProtectionLevel,
    val protectionFlags: Set<ProtectionFlag>,
    val description: String?,
    val declaringPackage: String,
)
