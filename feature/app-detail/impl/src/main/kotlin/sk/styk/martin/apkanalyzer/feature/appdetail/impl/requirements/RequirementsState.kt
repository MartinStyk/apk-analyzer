package sk.styk.martin.apkanalyzer.feature.appdetail.impl.requirements

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

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
    val availability: RequirementAvailability

    @Immutable
    data class Hardware(val name: String, override val availability: RequirementAvailability) : RequirementItem {
        override val identifier: String
            get() = name
    }

    @Immutable
    data class OpenGlEs(val versionName: String, val deviceVersionName: String?, override val identifier: String, override val availability: RequirementAvailability) : RequirementItem
}

enum class RequirementAvailability {
    Available,
    Missing,
    Unknown,
}
