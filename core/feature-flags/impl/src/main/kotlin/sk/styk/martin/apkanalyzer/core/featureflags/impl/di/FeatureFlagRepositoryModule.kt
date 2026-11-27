package sk.styk.martin.apkanalyzer.core.featureflags.impl.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepository
import sk.styk.martin.apkanalyzer.core.featureflags.impl.BuildConfig
import java.util.Optional
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class FeatureFlagRepositoryModule {
    @Provides
    @Singleton
    fun provideFeatureFlagRepository(
        @Named("buildFeatureFlags") defaults: FeatureFlagRepository,
        @Named("persistedFeatureFlags") persisted: Optional<FeatureFlagRepository>,
    ): FeatureFlagRepository = if (BuildConfig.DEBUG) persisted.orElse(defaults) else defaults
}
