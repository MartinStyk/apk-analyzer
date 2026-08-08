package sk.styk.martin.apkanalyzer.feature.apps.impl.components.apkfilepicker

import android.net.Uri

internal sealed interface ApkFilePickerAction {
    data object OpenPicker : ApkFilePickerAction
    data class ApkSelected(val uri: Uri) : ApkFilePickerAction
    data class ApkDetailOpened(val apkFilePath: String) : ApkFilePickerAction
}
