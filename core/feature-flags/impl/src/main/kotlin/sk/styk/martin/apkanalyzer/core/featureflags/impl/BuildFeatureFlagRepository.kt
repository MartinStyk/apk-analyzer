package sk.styk.martin.apkanalyzer.core.featureflags.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import sk.styk.martin.apkanalyzer.core.featureflags.api.ApplicationVariant
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlag
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BuildFeatureFlagRepository @Inject constructor(private val applicationVariant: ApplicationVariant) : FeatureFlagRepository {
    override fun observe(flag: FeatureFlag): Flow<Result<Boolean>> = flowOf(
        Result.success(defaultValue(flag)),
    )

    private fun defaultValue(flag: FeatureFlag): Boolean {
        if (!BuildConfig.DEBUG) return false

        return when (flag) {
            FeatureFlag.InventoryExport -> true
            FeatureFlag.WhatChangedTab -> applicationVariant == ApplicationVariant.Phone
            FeatureFlag.AppDetailHistory -> applicationVariant == ApplicationVariant.Phone
            FeatureFlag.AppHistoryRecording -> applicationVariant == ApplicationVariant.Phone
        }
    }
}
