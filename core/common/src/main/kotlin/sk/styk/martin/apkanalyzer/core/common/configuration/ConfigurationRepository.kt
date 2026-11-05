package sk.styk.martin.apkanalyzer.core.common.configuration

import kotlinx.coroutines.flow.Flow

interface ConfigurationRepository {
    fun <T : Any> observe(key: ConfigurationKey<T>): Flow<T>

    suspend fun <T : Any> get(key: ConfigurationKey<T>): T
}
