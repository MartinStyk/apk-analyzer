package sk.styk.martin.apkanalyzer.core.apphistory.capture.snapshot

import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.apps.packaging.InstalledSplitApk

@Serializable
internal data class InstalledSplitApkSnapshot(
    val fileName: String,
    val size: Long,
    val kind: String,
    val qualifier: String,
)

internal fun InstalledSplitApk.toSnapshot() = InstalledSplitApkSnapshot(
    fileName = fileName,
    size = size.bytes,
    kind = kind.name,
    qualifier = qualifier,
)
