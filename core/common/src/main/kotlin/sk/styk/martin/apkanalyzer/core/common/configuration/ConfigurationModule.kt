package sk.styk.martin.apkanalyzer.core.common.configuration

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ConfigurationModule {
    @Binds
    @Singleton
    fun bindConfigurationRepository(impl: BuildConfigurationRepository): ConfigurationRepository
}
