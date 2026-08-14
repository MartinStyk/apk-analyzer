package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal class FilterDraftBridge<T>(private val default: T) {

    private var pendingInput: T? = null

    private val resultChannel = Channel<T>(Channel.CONFLATED)
    val results: Flow<T> = resultChannel.receiveAsFlow()

    fun setInput(draft: T) {
        pendingInput = draft
    }

    fun consumeInput(): T = pendingInput?.also { pendingInput = null } ?: default

    fun submitResult(draft: T) {
        resultChannel.trySend(draft)
    }
}
