package sk.styk.martin.apkanalyzer.core.common.configuration

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import sk.styk.martin.apkanalyzer.core.common.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BuildConfigurationRepository @Inject constructor(private val applicationVariant: ApplicationVariant) : ConfigurationRepository {

    override fun <T : Any> observe(key: ConfigurationKey<T>): Flow<T> = flowOf(resolve(key))

    override suspend operator fun <T : Any> get(key: ConfigurationKey<T>): T = resolve(key)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> resolve(key: ConfigurationKey<T>): T = when (key) {
        ConfigurationKey.InventoryExportEnabled -> BuildConfig.DEBUG
        ConfigurationKey.AppHistoryDisplayEnabled -> BuildConfig.DEBUG && applicationVariant == ApplicationVariant.Phone
        ConfigurationKey.AppHistoryRecordingEnabled -> BuildConfig.DEBUG && applicationVariant == ApplicationVariant.Phone
    } as T
}
