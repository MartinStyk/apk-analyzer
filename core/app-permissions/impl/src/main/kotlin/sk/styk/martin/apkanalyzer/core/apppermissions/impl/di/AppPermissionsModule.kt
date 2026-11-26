package sk.styk.martin.apkanalyzer.core.apppermissions.impl.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.apppermissions.api.DevicePermissionsRepository
import sk.styk.martin.apkanalyzer.core.apppermissions.api.PermissionLabelProvider
import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.PermissionDescriptionProvider
import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.PermissionItemMapper
import sk.styk.martin.apkanalyzer.core.apppermissions.impl.DevicePermissionsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apppermissions.impl.PermissionLabelProviderImpl
import sk.styk.martin.apkanalyzer.core.apppermissions.impl.detail.PermissionDescriptionProviderImpl
import sk.styk.martin.apkanalyzer.core.apppermissions.impl.detail.PermissionItemMapperImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppPermissionsModule {
    @Binds
    @Singleton
    fun bindDevicePermissionsRepository(impl: DevicePermissionsRepositoryImpl): DevicePermissionsRepository

    @Binds
    @Singleton
    fun bindPermissionLabelProvider(impl: PermissionLabelProviderImpl): PermissionLabelProvider

    @Binds
    @Singleton
    fun bindPermissionDescriptionProvider(impl: PermissionDescriptionProviderImpl): PermissionDescriptionProvider

    @Binds
    @Singleton
    fun bindPermissionItemMapper(impl: PermissionItemMapperImpl): PermissionItemMapper
}
