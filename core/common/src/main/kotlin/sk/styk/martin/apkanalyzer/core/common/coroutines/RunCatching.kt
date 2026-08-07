package sk.styk.martin.apkanalyzer.core.common.coroutines

import kotlin.coroutines.cancellation.CancellationException

suspend inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (exception: CancellationException) {
    throw exception
} catch (throwable: Throwable) {
    Result.failure(throwable)
}
