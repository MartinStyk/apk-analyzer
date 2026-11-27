package sk.styk.martin.apkanalyzer.feature.settings.phone.flags

import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlag
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagOverride

internal sealed interface FeatureFlagsAction {
    data class SetOverride(val flag: FeatureFlag, val override: FeatureFlagOverride) : FeatureFlagsAction
    data object ResetAll : FeatureFlagsAction
    data object Retry : FeatureFlagsAction
    data object NavigateBack : FeatureFlagsAction
}
