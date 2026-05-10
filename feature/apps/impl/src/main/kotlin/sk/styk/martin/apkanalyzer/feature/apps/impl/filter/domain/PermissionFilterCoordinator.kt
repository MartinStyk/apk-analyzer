package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionFilterDraft(val selectedPermissions: Set<String> = setOf(), val matchAll: Boolean = false)

@Singleton
class PermissionFilterCoordinator @Inject constructor() {

    private var pendingInput: PermissionFilterDraft? = null

    private val resultChannel = Channel<PermissionFilterDraft>(Channel.CONFLATED)
    val results: Flow<PermissionFilterDraft> = resultChannel.receiveAsFlow()

    fun setInput(draft: PermissionFilterDraft) {
        pendingInput = draft
    }

    fun consumeInput(): PermissionFilterDraft = pendingInput?.also { pendingInput = null } ?: PermissionFilterDraft()

    fun submitResult(draft: PermissionFilterDraft) {
        resultChannel.trySend(draft)
    }
}
