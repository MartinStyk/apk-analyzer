package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionFilterDraft(val selectedPermissions: Set<String> = setOf(), val matchAll: Boolean = false)

@Singleton
class PermissionFilterCoordinator @Inject constructor() {

    private val bridge = FilterDraftBridge(PermissionFilterDraft())
    val results: Flow<PermissionFilterDraft> = bridge.results

    fun setInput(draft: PermissionFilterDraft) = bridge.setInput(draft)

    fun consumeInput(): PermissionFilterDraft = bridge.consumeInput()

    fun submitResult(draft: PermissionFilterDraft) = bridge.submitResult(draft)
}
