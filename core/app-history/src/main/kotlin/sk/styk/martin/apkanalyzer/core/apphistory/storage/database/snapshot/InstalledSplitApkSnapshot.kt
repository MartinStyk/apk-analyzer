package sk.styk.martin.apkanalyzer.core.apphistory.storage.database.snapshot

import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.apps.packaging.InstalledSplitApk
import sk.styk.martin.apkanalyzer.core.apps.packaging.SplitApkKind
import sk.styk.martin.apkanalyzer.core.common.model.bytes

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

internal fun InstalledSplitApkSnapshot.toDomain() = InstalledSplitApk(
    fileName = fileName,
    filePath = null,
    size = size.bytes,
    kind = SplitApkKind.valueOf(kind),
    qualifier = qualifier,
)
