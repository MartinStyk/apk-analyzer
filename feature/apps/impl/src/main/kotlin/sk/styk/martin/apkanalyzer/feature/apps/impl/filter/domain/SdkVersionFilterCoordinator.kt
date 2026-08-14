package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class SdkVersionFilterDraft(val selectedSdkVersions: Set<Int> = setOf())

@Singleton
class SdkVersionFilterCoordinator @Inject constructor() {

    private val bridge = FilterDraftBridge(SdkVersionFilterDraft())
    val results: Flow<SdkVersionFilterDraft> = bridge.results

    fun setInput(draft: SdkVersionFilterDraft) = bridge.setInput(draft)

    fun consumeInput(): SdkVersionFilterDraft = bridge.consumeInput()

    fun submitResult(draft: SdkVersionFilterDraft) = bridge.submitResult(draft)
}
