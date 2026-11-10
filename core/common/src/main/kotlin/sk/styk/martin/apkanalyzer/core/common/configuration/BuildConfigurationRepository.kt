package sk.styk.martin.apkanalyzer.core.common.configuration

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import sk.styk.martin.apkanalyzer.core.common.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BuildConfigurationRepository @Inject constructor() : ConfigurationRepository {
    override fun <T : Any> observe(key: ConfigurationKey<T>): Flow<T> = flowOf(resolve(key))

    override suspend fun <T : Any> get(key: ConfigurationKey<T>): T = resolve(key)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> resolve(key: ConfigurationKey<T>): T = when (key) {
        ConfigurationKey.InventoryExportEnabled -> BuildConfig.DEBUG
        ConfigurationKey.AppHistoryDisplayEnabled -> BuildConfig.DEBUG
        ConfigurationKey.AppHistoryRecordingEnabled -> BuildConfig.DEBUG
    } as T
}
