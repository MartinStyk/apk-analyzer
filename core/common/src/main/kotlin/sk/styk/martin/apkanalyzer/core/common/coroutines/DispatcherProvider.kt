package sk.styk.martin.apkanalyzer.core.common.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class DispatcherProvider
@Inject
constructor() {
    fun main(): CoroutineDispatcher = Dispatchers.Main

    fun default(): CoroutineDispatcher = Dispatchers.Default

    fun io(): CoroutineDispatcher = Dispatchers.IO

    fun unconfined(): CoroutineDispatcher = Dispatchers.Unconfined
}
