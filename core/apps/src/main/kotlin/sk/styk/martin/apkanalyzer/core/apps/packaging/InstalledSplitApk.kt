package sk.styk.martin.apkanalyzer.core.apps.packaging

import sk.styk.martin.apkanalyzer.core.common.model.AppSize

data class InstalledSplitApk(
    val fileName: String,
    val filePath: String,
    val size: AppSize,
    val kind: SplitApkKind,
    val qualifier: String,
)

enum class SplitApkKind {
    DynamicFeature,
    Abi,
    ScreenDensity,
    Language,
}
