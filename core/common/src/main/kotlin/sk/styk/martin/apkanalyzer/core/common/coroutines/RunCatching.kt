package sk.styk.martin.apkanalyzer.core.common.coroutines

import kotlin.coroutines.cancellation.CancellationException

@Suppress("TooGenericExceptionCaught")
suspend inline fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (exception: CancellationException) {
    throw exception
} catch (throwable: Throwable) {
    Result.failure(throwable)
}
