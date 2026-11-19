package sk.styk.martin.apkanalyzer.core.common.configuration

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class ConfigurationRepositoryFake(vararg enabledKeys: ConfigurationKey<Boolean>) : ConfigurationRepository {
    private val enabledKeys = MutableStateFlow(enabledKeys.toSet())

    operator fun set(key: ConfigurationKey<Boolean>, enabled: Boolean) {
        enabledKeys.update { if (enabled) it + key else it - key }
    }

    override fun <T : Any> observe(key: ConfigurationKey<T>): Flow<T> = enabledKeys.map { resolve(key, it) }

    override suspend operator fun <T : Any> get(key: ConfigurationKey<T>): T = resolve(key, enabledKeys.value)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> resolve(key: ConfigurationKey<T>, enabled: Set<ConfigurationKey<Boolean>>): T = when (key) {
        ConfigurationKey.InventoryExportEnabled -> ConfigurationKey.InventoryExportEnabled in enabled
        ConfigurationKey.AppHistoryDisplayEnabled -> ConfigurationKey.AppHistoryDisplayEnabled in enabled
        ConfigurationKey.AppHistoryRecordingEnabled -> ConfigurationKey.AppHistoryRecordingEnabled in enabled
    } as T
}
