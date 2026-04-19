package sk.styk.martin.apkanalyzer.core.common.settings

import kotlinx.coroutines.flow.Flow

interface PersistenceRepository {
    fun <T> observe(key: Key<T>): Flow<T>

    suspend fun <T> save(key: Key<T>, value: T)
}
