package sk.styk.martin.apkanalyzer.core.common.settings

import kotlinx.coroutines.flow.Flow

interface PersistenceRepository {
    fun <T : Any> observe(key: Key<T>): Flow<T>

    suspend fun <T : Any> get(key: Key<T>): T

    suspend fun <T : Any> save(key: Key<T>, value: T)
}
