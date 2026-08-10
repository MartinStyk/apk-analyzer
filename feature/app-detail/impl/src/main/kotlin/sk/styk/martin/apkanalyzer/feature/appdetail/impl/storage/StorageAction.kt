package sk.styk.martin.apkanalyzer.feature.appdetail.impl.storage

internal sealed interface StorageAction {
    data object Retry : StorageAction

    data object Back : StorageAction

    data object OpenPermissionSettings : StorageAction

    data class CopyValue(val label: String, val value: String) : StorageAction
}
