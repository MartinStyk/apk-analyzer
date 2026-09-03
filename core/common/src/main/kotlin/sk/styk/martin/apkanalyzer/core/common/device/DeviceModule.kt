package sk.styk.martin.apkanalyzer.core.common.device

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DeviceModule {
    @Binds
    @Singleton
    fun bindDeviceIdProvider(impl: DeviceIdProviderImpl): DeviceIdProvider
}
