package sk.styk.martin.apkanalyzer.core.featureflags.debug

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlag
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagOverride
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagOverridesRepository
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepository
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagStatus
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class DataStoreFeatureFlagRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @Named("buildFeatureFlags") private val defaults: FeatureFlagRepository,
) : FeatureFlagRepository,
    FeatureFlagOverridesRepository {
    override fun observe(flag: FeatureFlag): Flow<Boolean> = combine(defaults.observe(flag), dataStore.data) { defaultEnabled, preferences ->
        status(flag, defaultEnabled, preferences).isEnabled
    }
        .distinctUntilChanged()

    override fun observe(): Flow<List<FeatureFlagStatus>> = flow {
        val defaultValues = FeatureFlag.entries.associateWith { defaults[it] }
        emitAll(
            dataStore.data.map { preferences ->
                defaultValues.map { (flag, defaultEnabled) -> status(flag, defaultEnabled, preferences) }
            },
        )
    }

    override suspend fun set(flag: FeatureFlag, override: FeatureFlagOverride) {
        dataStore.edit { preferences ->
            when (override) {
                FeatureFlagOverride.Default -> preferences.remove(flag.preferenceKey())
                FeatureFlagOverride.Enabled -> preferences[flag.preferenceKey()] = true
                FeatureFlagOverride.Disabled -> preferences[flag.preferenceKey()] = false
            }
        }
    }

    override suspend fun resetAll() {
        dataStore.edit { preferences ->
            FeatureFlag.entries.forEach { preferences.remove(it.preferenceKey()) }
        }
    }

    private fun status(
        flag: FeatureFlag,
        defaultEnabled: Boolean,
        preferences: Preferences,
    ): FeatureFlagStatus = FeatureFlagStatus(
        flag = flag,
        override = when (preferences[flag.preferenceKey()]) {
            true -> FeatureFlagOverride.Enabled
            false -> FeatureFlagOverride.Disabled
            null -> FeatureFlagOverride.Default
        },
        defaultEnabled = defaultEnabled,
    )

    private fun FeatureFlag.preferenceKey(): Preferences.Key<Boolean> = booleanPreferencesKey(
        when (this) {
            FeatureFlag.InventoryExport -> "inventory_export"
            FeatureFlag.WhatChangedTab -> "what_changed_tab"
            FeatureFlag.AppDetailHistory -> "app_history_display"
            FeatureFlag.AppHistoryRecording -> "app_history_recording"
        },
    )
}
