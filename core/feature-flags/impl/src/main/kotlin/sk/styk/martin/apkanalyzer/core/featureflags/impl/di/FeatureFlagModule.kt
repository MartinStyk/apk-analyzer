package sk.styk.martin.apkanalyzer.core.featureflags.impl.di

import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagOverridesRepository
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepository
import sk.styk.martin.apkanalyzer.core.featureflags.impl.BuildFeatureFlagRepository
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
internal interface FeatureFlagModule {
    @Binds
    @Named("buildFeatureFlags")
    fun bindBuildFeatureFlagRepository(impl: BuildFeatureFlagRepository): FeatureFlagRepository

    @BindsOptionalOf
    @Named("persistedFeatureFlags")
    fun optionalPersistedFeatureFlagRepository(): FeatureFlagRepository

    @BindsOptionalOf
    fun optionalFeatureFlagOverridesRepository(): FeatureFlagOverridesRepository
}
