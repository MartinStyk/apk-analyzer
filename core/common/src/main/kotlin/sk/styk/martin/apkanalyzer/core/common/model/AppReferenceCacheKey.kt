package sk.styk.martin.apkanalyzer.core.common.model

import java.io.File

sealed interface AppReferenceCacheKey {
    data class InstalledPackage(val packageName: PackageName) : AppReferenceCacheKey
    data class ApkFile(val path: String, val lastModified: Long) : AppReferenceCacheKey
}

fun AppReference.toCacheKey(): AppReferenceCacheKey = when (this) {
    is AppReference.InstalledPackage -> AppReferenceCacheKey.InstalledPackage(packageName)
    is AppReference.ApkFile -> AppReferenceCacheKey.ApkFile(path, File(path).lastModified())
}
