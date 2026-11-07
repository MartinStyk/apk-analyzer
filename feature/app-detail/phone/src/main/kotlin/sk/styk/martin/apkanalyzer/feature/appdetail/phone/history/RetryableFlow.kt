package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

internal class RetryableFlow<T>(loading: T, source: () -> Flow<T>) {
    private val attempts = MutableStateFlow(0)
    private var startedAttempt = 0

    val flow: Flow<T> = attempts.flatMapLatest { attempt ->
        source().onStart {
            if (attempt != startedAttempt) {
                startedAttempt = attempt
                emit(loading)
            }
        }
    }

    fun retry() = attempts.update { it + 1 }
}
