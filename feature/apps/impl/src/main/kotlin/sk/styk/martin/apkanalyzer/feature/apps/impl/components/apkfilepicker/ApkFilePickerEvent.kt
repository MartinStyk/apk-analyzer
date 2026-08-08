package sk.styk.martin.apkanalyzer.feature.apps.impl.components.apkfilepicker

internal sealed interface ApkFilePickerEvent {
    data object OpenDocument : ApkFilePickerEvent
    data class OpenApkDetail(val apkFilePath: String) : ApkFilePickerEvent
    data object ShowOpenError : ApkFilePickerEvent
}
