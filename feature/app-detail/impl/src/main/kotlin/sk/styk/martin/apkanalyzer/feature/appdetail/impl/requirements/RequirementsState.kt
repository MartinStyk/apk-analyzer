package sk.styk.martin.apkanalyzer.feature.appdetail.impl.requirements

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import sk.styk.martin.apkanalyzer.core.apps.model.FeatureAvailability

@Immutable
sealed interface RequirementsState {
    data object Loading : RequirementsState

    @Immutable
    data class Loaded(val sections: ImmutableList<RequirementSection>, val missingRequiredCount: Int) : RequirementsState {
        val hasRequirements: Boolean
            get() = sections.isNotEmpty()
    }

    data object Error : RequirementsState
}

@Immutable
data class RequirementSection(val isRequired: Boolean, val requirements: ImmutableList<RequirementItem>)

@Immutable
sealed interface RequirementItem {
    val identifier: String
    val isRequired: Boolean
    val availability: FeatureAvailability

    @Immutable
    data class Hardware(
        val name: String,
        val requiredVersion: Int,
        val deviceVersion: Int?,
        override val isRequired: Boolean,
        override val availability: FeatureAvailability,
    ) : RequirementItem {
        override val identifier: String
            get() = name

        val isVersionMiss: Boolean
            get() = availability == FeatureAvailability.Missing && deviceVersion != null
    }

    @Immutable
    data class OpenGlEs(
        val versionName: String,
        val deviceVersionName: String?,
        override val identifier: String,
        override val isRequired: Boolean,
        override val availability: FeatureAvailability,
    ) : RequirementItem
}
