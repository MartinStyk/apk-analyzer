package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.aisummary

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiDescription

internal sealed interface AiSummaryState {
    data object Hidden : AiSummaryState
    data object Downloadable : AiSummaryState
    data object Downloading : AiSummaryState
    data object Loading : AiSummaryState

    @Immutable
    data class Loaded(val description: AppAiDescription) : AiSummaryState
}
