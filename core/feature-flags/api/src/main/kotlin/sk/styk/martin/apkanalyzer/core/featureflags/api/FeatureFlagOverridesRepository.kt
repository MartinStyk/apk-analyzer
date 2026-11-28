package sk.styk.martin.apkanalyzer.core.featureflags.api

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

interface FeatureFlagOverridesRepository {
    fun observe(): Flow<List<FeatureFlagStatus>>

    suspend operator fun set(flag: FeatureFlag, override: FeatureFlagOverride)

    suspend fun resetAll()
}

@Serializable
enum class FeatureFlagOverride {
    Default,
    Enabled,
    Disabled,
}

@Serializable
data class FeatureFlagStatus(
    val flag: FeatureFlag,
    val override: FeatureFlagOverride,
    val defaultEnabled: Boolean,
) {
    val isEnabled: Boolean
        get() = when (override) {
            FeatureFlagOverride.Default -> defaultEnabled
            FeatureFlagOverride.Enabled -> true
            FeatureFlagOverride.Disabled -> false
        }
}
