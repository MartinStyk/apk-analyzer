package sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.apphistory.api.storage.AppHistoryStorageRepository
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryPersistenceRepository
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryPersistenceRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryStorageRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppHistoryPersistenceModule {
    @Binds
    @Singleton
    fun bindAppHistoryPersistenceRepository(impl: AppHistoryPersistenceRepositoryImpl): AppHistoryPersistenceRepository

    @Binds
    @Singleton
    fun bindAppHistoryStorageRepository(impl: AppHistoryStorageRepositoryImpl): AppHistoryStorageRepository
}
