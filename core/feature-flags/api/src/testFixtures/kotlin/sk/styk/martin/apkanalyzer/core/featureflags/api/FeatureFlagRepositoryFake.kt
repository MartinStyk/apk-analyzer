package sk.styk.martin.apkanalyzer.core.featureflags.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FeatureFlagRepositoryFake(vararg enabledFlags: FeatureFlag) : FeatureFlagRepository {
    private val enabledFlags = MutableStateFlow(enabledFlags.toSet())

    operator fun set(flag: FeatureFlag, enabled: Boolean) {
        enabledFlags.update { if (enabled) it + flag else it - flag }
    }

    override fun observe(flag: FeatureFlag): Flow<Boolean> = enabledFlags
        .map { flag in it }
        .distinctUntilChanged()
}
