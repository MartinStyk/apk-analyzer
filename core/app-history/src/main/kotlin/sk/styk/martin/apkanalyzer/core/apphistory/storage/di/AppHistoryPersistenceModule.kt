package sk.styk.martin.apkanalyzer.core.apphistory.storage.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryPersistenceRepository
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryPersistenceRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppHistoryPersistenceModule {
    @Binds
    @Singleton
    fun bindAppHistoryPersistenceRepository(impl: AppHistoryPersistenceRepositoryImpl): AppHistoryPersistenceRepository
}
