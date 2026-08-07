package sk.styk.martin.apkanalyzer.core.common.model

sealed interface AppReference {
    data class InstalledPackage(val packageName: String) : AppReference
    data class ApkFile(val path: String) : AppReference
}
