package sk.styk.martin.apkanalyzer.core.common.coroutines

import kotlin.coroutines.cancellation.CancellationException

inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = runCatching(block).onFailure {
    if (it is CancellationException) {
        throw it
    }
}

suspend inline fun <T> runSuspendCatchingCancellable(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (exception: CancellationException) {
    throw exception
} catch (throwable: Throwable) {
    Result.failure(throwable)
}
