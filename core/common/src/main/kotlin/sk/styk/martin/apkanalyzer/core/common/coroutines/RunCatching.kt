package sk.styk.martin.apkanalyzer.core.common.coroutines

import kotlin.coroutines.cancellation.CancellationException

suspend inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = runCatching(block).onFailure {
    if (it is CancellationException) {
        throw it
    }
}
