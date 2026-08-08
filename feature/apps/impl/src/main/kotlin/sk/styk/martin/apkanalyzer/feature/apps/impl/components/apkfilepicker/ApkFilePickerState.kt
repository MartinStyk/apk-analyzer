package sk.styk.martin.apkanalyzer.feature.apps.impl.components.apkfilepicker

internal sealed interface ApkFilePickerState {
    data object Ready : ApkFilePickerState
    data object Copying : ApkFilePickerState
}
