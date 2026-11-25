package sk.styk.martin.apkanalyzer.feature.appdetail.tv.requirements

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Requirements

@Immutable
internal sealed interface TvRequirementsState {
    data object Loading : TvRequirementsState

    data object Error : TvRequirementsState

    @Immutable
    data class Loaded(val requirements: Requirements) : TvRequirementsState
}
