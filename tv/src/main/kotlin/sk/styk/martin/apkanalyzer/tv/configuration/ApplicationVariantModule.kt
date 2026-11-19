package sk.styk.martin.apkanalyzer.tv.configuration

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.common.configuration.ApplicationVariant

@Module
@InstallIn(SingletonComponent::class)
internal class ApplicationVariantModule {
    @Provides
    fun provideApplicationVariant(): ApplicationVariant = ApplicationVariant.Tv
}
