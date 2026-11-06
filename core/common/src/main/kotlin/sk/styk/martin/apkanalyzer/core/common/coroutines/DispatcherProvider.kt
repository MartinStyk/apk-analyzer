package sk.styk.martin.apkanalyzer.core.common.coroutines

import kotlinx.coroutines.CoroutineDispatcher

interface DispatcherProvider {
    fun main(): CoroutineDispatcher

    fun default(): CoroutineDispatcher

    fun io(): CoroutineDispatcher

    fun unconfined(): CoroutineDispatcher
}
