package sk.styk.martin.apkanalyzer.core.featureflags.api

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

interface FeatureFlagOverridesRepository {
    fun observe(): Flow<Result<List<FeatureFlagStatus>>>

    suspend fun set(flag: FeatureFlag, override: FeatureFlagOverride): Result<Unit>

    suspend fun resetAll(): Result<Unit>
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
