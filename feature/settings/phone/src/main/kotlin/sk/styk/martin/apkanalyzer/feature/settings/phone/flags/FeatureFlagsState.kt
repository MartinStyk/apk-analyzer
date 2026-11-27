package sk.styk.martin.apkanalyzer.feature.settings.phone.flags

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagStatus

@Immutable
internal sealed interface FeatureFlagsState {
    data object Loading : FeatureFlagsState
    data object Error : FeatureFlagsState
    data class Content(val flags: List<FeatureFlagStatus>, val isSaving: Boolean = false) : FeatureFlagsState
}
