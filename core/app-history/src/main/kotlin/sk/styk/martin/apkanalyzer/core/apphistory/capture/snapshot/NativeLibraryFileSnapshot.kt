package sk.styk.martin.apkanalyzer.core.apphistory.capture.snapshot

import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryFile

@Serializable
internal data class NativeLibraryFileSnapshot(
    val name: String,
    val abi: String,
    val size: Long,
    val containingApkFileName: String,
)

internal fun NativeLibraryFile.toSnapshot() = NativeLibraryFileSnapshot(
    name = name,
    abi = abi,
    size = size.bytes,
    containingApkFileName = containingApkFileName,
)
