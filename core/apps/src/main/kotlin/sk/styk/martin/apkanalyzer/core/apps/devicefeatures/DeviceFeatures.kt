package sk.styk.martin.apkanalyzer.core.apps.devicefeatures

data class DeviceFeatures(val featureVersions: Map<String, Int>, val openGlEsVersion: Int?) {
    val isKnown: Boolean
        get() = featureVersions.isNotEmpty()

    val openGlEsVersionName: String?
        get() = openGlEsVersion?.let { Feature.OpenGlEs.versionName(it) }

    fun versionOf(featureName: String): Int? = featureVersions[featureName]

    fun availabilityOf(feature: Feature): FeatureAvailability = if (!isKnown) {
        FeatureAvailability.Unknown
    } else {
        when (feature) {
            is Feature.Hardware -> featureVersions[feature.name]
                ?.let { availabilityOf(required = feature.version, onDevice = it) }
                ?: FeatureAvailability.Missing

            is Feature.OpenGlEs -> when (openGlEsVersion) {
                null -> FeatureAvailability.Unknown
                else -> availabilityOf(required = feature.reqGlEsVersion, onDevice = openGlEsVersion)
            }
        }
    }

    private fun availabilityOf(required: Int, onDevice: Int) = when {
        onDevice >= required -> FeatureAvailability.Available
        else -> FeatureAvailability.Missing
    }

    companion object {
        val Unknown = DeviceFeatures(featureVersions = emptyMap(), openGlEsVersion = null)
    }
}
