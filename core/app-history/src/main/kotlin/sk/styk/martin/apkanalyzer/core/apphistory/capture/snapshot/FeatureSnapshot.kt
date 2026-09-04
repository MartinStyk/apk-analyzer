package sk.styk.martin.apkanalyzer.core.apphistory.capture.snapshot

import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature

@Serializable
internal sealed interface FeatureSnapshot {

    @Serializable
    data class Hardware(
        val name: String,
        val version: Int,
        val isRequired: Boolean,
    ) : FeatureSnapshot

    @Serializable
    data class OpenGlEs(val reqGlEsVersion: Int, val isRequired: Boolean) : FeatureSnapshot
}

internal fun Feature.toSnapshot(): FeatureSnapshot = when (this) {
    is Feature.Hardware -> FeatureSnapshot.Hardware(name = name, version = version, isRequired = isRequired)
    is Feature.OpenGlEs -> FeatureSnapshot.OpenGlEs(reqGlEsVersion = reqGlEsVersion, isRequired = isRequired)
}
