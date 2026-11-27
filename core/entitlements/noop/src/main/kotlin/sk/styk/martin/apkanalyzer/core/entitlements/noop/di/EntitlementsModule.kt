package sk.styk.martin.apkanalyzer.core.entitlements.noop.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.entitlements.api.EntitlementsRepository
import sk.styk.martin.apkanalyzer.core.entitlements.noop.EntitlementsRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface EntitlementsModule {
    @Binds
    @Singleton
    fun bindEntitlementsRepository(impl: EntitlementsRepositoryImpl): EntitlementsRepository
}
