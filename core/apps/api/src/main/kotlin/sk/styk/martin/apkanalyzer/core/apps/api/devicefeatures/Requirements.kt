package sk.styk.martin.apkanalyzer.core.apps.devicefeatures

import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import java.util.Locale

data class Requirements(val sections: List<RequirementSection>, val missingRequiredCount: Int)

data class RequirementSection(val isRequired: Boolean, val requirements: List<RequirementItem>)

sealed interface RequirementItem {
    val identifier: String
    val isRequired: Boolean
    val availability: FeatureAvailability

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

    data class OpenGlEs(
        val versionName: String,
        val deviceVersionName: String?,
        override val identifier: String,
        override val isRequired: Boolean,
        override val availability: FeatureAvailability,
    ) : RequirementItem
}

fun AppDetail.requirements(deviceFeatures: DeviceFeatures): Requirements {
    val (required, optional) = features
        .map { it.toRequirementItem(deviceFeatures) }
        .sortedByDescending { it.isRequired }
        .distinctBy { it.identifier }
        .partition { it.isRequired }

    return Requirements(
        sections = listOfNotNull(
            required.toSectionOrNull(isRequired = true),
            optional.toSectionOrNull(isRequired = false),
        ),
        missingRequiredCount = required.count { it.availability == FeatureAvailability.Missing },
    )
}

private fun List<RequirementItem>.toSectionOrNull(isRequired: Boolean) = takeIf { it.isNotEmpty() }
    ?.let { RequirementSection(isRequired = isRequired, requirements = it.sortedWith(requirementOrder)) }

private val requirementOrder = compareBy<RequirementItem>(
    { it.availability != FeatureAvailability.Missing },
    { it is RequirementItem.OpenGlEs },
    { it.identifier },
)

fun Feature.toRequirementItem(deviceFeatures: DeviceFeatures): RequirementItem {
    val availability = deviceFeatures.availabilityOf(this)
    return when (this) {
        is Feature.Hardware -> RequirementItem.Hardware(
            name = name,
            requiredVersion = version,
            deviceVersion = deviceFeatures.versionOf(name),
            isRequired = isRequired,
            availability = availability,
        )

        is Feature.OpenGlEs -> RequirementItem.OpenGlEs(
            versionName = versionName,
            deviceVersionName = deviceFeatures.openGlEsVersionName,
            identifier = "0x%08X".format(Locale.ROOT, reqGlEsVersion),
            isRequired = isRequired,
            availability = availability,
        )
    }
}
