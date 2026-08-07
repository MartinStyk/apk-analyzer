package sk.styk.martin.apkanalyzer.core.appindex.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.appindex.AppIndexRepository
import sk.styk.martin.apkanalyzer.core.appindex.AppIndexRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppIndexModule {
    @Binds
    @Singleton
    fun bindAppIndexRepository(impl: AppIndexRepositoryImpl): AppIndexRepository
}
