package sk.styk.martin.apkanalyzer.core.apps.model

data class ContentProvider(val name: String, val authority: String? = null, val readPermission: String? = null, val writePermission: String? = null, val isExported: Boolean = false)

val ContentProvider.isExternallyReachableWithoutPermission: Boolean
    get() = isExported && (readPermission.isNullOrBlank() || writePermission.isNullOrBlank())
