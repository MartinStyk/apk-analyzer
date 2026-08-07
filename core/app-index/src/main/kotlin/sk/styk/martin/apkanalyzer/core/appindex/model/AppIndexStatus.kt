package sk.styk.martin.apkanalyzer.core.appindex.model

sealed interface AppIndexStatus {
    data object Loading : AppIndexStatus
    data class Data(val index: AppAttributeIndex) : AppIndexStatus
}
