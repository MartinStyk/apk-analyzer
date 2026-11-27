package sk.styk.martin.apkanalyzer.core.featureflags.debug.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagOverridesRepository
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepository
import sk.styk.martin.apkanalyzer.core.featureflags.debug.DataStoreFeatureFlagRepository
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FeatureFlagOverridesModule {
    @Binds
    @Singleton
    @Named("persistedFeatureFlags")
    fun bindPersistedFeatureFlagRepository(impl: DataStoreFeatureFlagRepository): FeatureFlagRepository

    @Binds
    @Singleton
    fun bindFeatureFlagOverridesRepository(impl: DataStoreFeatureFlagRepository): FeatureFlagOverridesRepository
}
