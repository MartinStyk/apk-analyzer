package sk.styk.martin.apkanalyzer.core.apps.model

import sk.styk.martin.apkanalyzer.core.common.model.AppSize

data class NativeLibraries(val files: List<NativeLibraryFile>) {
    val abis: List<String> get() = files.map { it.abi }.distinct().sorted()
    val libraryNames: List<String> get() = files.map { it.name }.distinct().sorted()
    val hasNativeCode: Boolean get() = files.isNotEmpty()

    companion object {
        val Empty = NativeLibraries(files = emptyList())
    }
}

data class NativeLibraryFile(
    val name: String,
    val abi: String,
    val size: AppSize,
    val containingApkFileName: String,
)
