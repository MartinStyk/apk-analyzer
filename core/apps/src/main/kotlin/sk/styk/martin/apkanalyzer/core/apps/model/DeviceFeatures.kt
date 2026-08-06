package sk.styk.martin.apkanalyzer.core.apps.model

data class DeviceFeatures(val featureNames: Set<String>, val openGlEsVersion: Int?) {
    val isKnown: Boolean
        get() = featureNames.isNotEmpty()

    val openGlEsVersionName: String?
        get() = openGlEsVersion?.let { Feature.OpenGlEs.versionName(it) }

    fun supports(feature: Feature): Boolean? {
        if (!isKnown) return null
        return when (feature) {
            is Feature.Hardware -> feature.name in featureNames
            is Feature.OpenGlEs -> openGlEsVersion?.let { it >= feature.reqGlEsVersion }
        }
    }

    companion object {
        val Unknown = DeviceFeatures(featureNames = emptySet(), openGlEsVersion = null)
    }
}
