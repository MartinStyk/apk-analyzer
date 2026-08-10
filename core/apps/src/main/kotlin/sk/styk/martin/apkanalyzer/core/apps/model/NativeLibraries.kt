package sk.styk.martin.apkanalyzer.core.apps.model

data class NativeLibraries(val abis: List<String>, val libraryNames: List<String>) {
    val hasNativeCode: Boolean
        get() = abis.isNotEmpty()

    companion object {
        val Empty = NativeLibraries(abis = emptyList(), libraryNames = emptyList())
    }
}
