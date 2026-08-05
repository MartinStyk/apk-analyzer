package sk.styk.martin.apkanalyzer.core.apps.model

data class Permission(
    val name: String,
    val simpleName: String,
    val groupName: String? = null,
    val protectionLevel: ProtectionLevel = ProtectionLevel.Normal,
    val protectionFlags: Set<ProtectionFlag> = emptySet(),
    val description: String? = null,
    val declaringPackage: String? = null,
)
