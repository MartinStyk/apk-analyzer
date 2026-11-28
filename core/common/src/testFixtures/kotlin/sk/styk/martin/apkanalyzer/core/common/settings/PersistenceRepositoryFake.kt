package sk.styk.martin.apkanalyzer.core.common.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class PersistenceRepositoryFake(vararg values: Pair<Key<*>, Any>) : PersistenceRepository {
    private val values = MutableStateFlow(values.toMap())

    operator fun <T : Any> set(key: Key<T>, value: T) {
        this.values.update { it + (key to value) }
    }

    override fun <T : Any> observe(key: Key<T>): Flow<T> = values.map { it.valueOf(key) }.distinctUntilChanged()

    override suspend fun <T : Any> get(key: Key<T>): T = values.value.valueOf(key)

    override suspend fun <T : Any> save(key: Key<T>, value: T) {
        set(key, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> Map<Key<*>, Any>.valueOf(key: Key<T>): T = checkNotNull(get(key)) { "Set $key in PersistenceRepositoryFake before reading it" } as T
}
