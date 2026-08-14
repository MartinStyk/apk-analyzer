package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import javax.inject.Inject
import javax.inject.Singleton

data class SourceFilterDraft(val selectedSources: Set<AppSource> = setOf())

@Singleton
class SourceFilterCoordinator @Inject constructor() {

    private val bridge = FilterDraftBridge(SourceFilterDraft())
    val results: Flow<SourceFilterDraft> = bridge.results

    fun setInput(draft: SourceFilterDraft) = bridge.setInput(draft)

    fun consumeInput(): SourceFilterDraft = bridge.consumeInput()

    fun submitResult(draft: SourceFilterDraft) = bridge.submitResult(draft)
}
