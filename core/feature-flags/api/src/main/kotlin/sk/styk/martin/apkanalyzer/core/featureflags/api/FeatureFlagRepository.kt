package sk.styk.martin.apkanalyzer.core.featureflags.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface FeatureFlagRepository {
    fun observe(flag: FeatureFlag): Flow<Result<Boolean>>

    suspend operator fun get(flag: FeatureFlag): Result<Boolean> = observe(flag).first()
}
