package sk.styk.martin.apkanalyzer.core.apps.components

data class ContentProvider(
    val name: String,
    val authority: String? = null,
    val readPermission: String? = null,
    val writePermission: String? = null,
    val isExported: Boolean = false,
    val pathPermissions: List<ProviderPathPermission> = emptyList(),
)

data class ProviderPathPermission(
    val path: String,
    val matchType: ProviderPathMatchType,
    val readPermission: String? = null,
    val writePermission: String? = null,
)

enum class ProviderPathMatchType {
    Literal,
    Prefix,
    SimpleGlob,
    AdvancedGlob,
    Suffix,
}

val ContentProvider.hasUnguardedPathPermission: Boolean
    get() = pathPermissions.any { it.readPermission.isNullOrBlank() || it.writePermission.isNullOrBlank() }

val ContentProvider.isExternallyReachableWithoutPermission: Boolean
    get() = isExported &&
        (readPermission.isNullOrBlank() || writePermission.isNullOrBlank() || hasUnguardedPathPermission)
