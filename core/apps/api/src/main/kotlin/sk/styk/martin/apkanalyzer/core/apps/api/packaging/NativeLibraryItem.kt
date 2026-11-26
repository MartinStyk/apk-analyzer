package sk.styk.martin.apkanalyzer.core.apps.packaging

import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.bytes

data class NativeLibraryItem(
    val name: String,
    val abis: List<String>,
    val totalSize: AppSize,
    val isDeviceCompatible: Boolean,
    val variants: List<NativeLibraryVariant>,
)

data class NativeLibraryVariant(
    val abi: String,
    val size: AppSize,
    val containingApkFileName: String,
)

fun List<NativeLibraryFile>.groupedByLibrary(deviceAbis: Set<String>): List<NativeLibraryItem> = groupBy { it.name }
    .map { (name, files) ->
        val abis = files.map { it.abi }.distinct().sorted()
        NativeLibraryItem(
            name = name,
            abis = abis,
            totalSize = files.fold(0.bytes) { acc, file -> acc + file.size },
            isDeviceCompatible = deviceAbis.isEmpty() || abis.any { it in deviceAbis },
            variants = files.sortedBy { it.abi }
                .map { NativeLibraryVariant(abi = it.abi, size = it.size, containingApkFileName = it.containingApkFileName) },
        )
    }
    .sortedBy { it.name.lowercase() }
