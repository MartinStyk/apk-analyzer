package sk.styk.martin.apkanalyzer.feature.appdetail.impl.nativelibraries

internal sealed interface NativeLibrariesAction {
    data object Retry : NativeLibrariesAction

    data object Back : NativeLibrariesAction

    data class ChangeQuery(val query: String) : NativeLibrariesAction

    data object ClearQuery : NativeLibrariesAction

    data class CopyValue(val label: String, val value: String) : NativeLibrariesAction
}
