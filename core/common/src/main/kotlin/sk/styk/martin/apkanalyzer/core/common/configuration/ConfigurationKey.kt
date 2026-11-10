package sk.styk.martin.apkanalyzer.core.common.configuration

sealed interface ConfigurationKey<T : Any> {
    data object InventoryExportEnabled : ConfigurationKey<Boolean>

    data object AppHistoryDisplayEnabled : ConfigurationKey<Boolean>

    data object AppHistoryRecordingEnabled : ConfigurationKey<Boolean>
}
