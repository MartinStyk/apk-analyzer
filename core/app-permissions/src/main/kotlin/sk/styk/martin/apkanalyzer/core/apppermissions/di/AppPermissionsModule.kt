package sk.styk.martin.apkanalyzer.core.apppermissions.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.apppermissions.DevicePermissionsRepository
import sk.styk.martin.apkanalyzer.core.apppermissions.DevicePermissionsRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppPermissionsModule {
    @Binds
    @Singleton
    fun bindDevicePermissionsRepository(impl: DevicePermissionsRepositoryImpl): DevicePermissionsRepository
}
