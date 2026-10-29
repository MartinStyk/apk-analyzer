package sk.styk.martin.apkanalyzer.core.apphistory.timeline.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.AppHistoryRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppHistoryTimelineModule {
    @Binds
    @Singleton
    fun bindAppHistoryRepository(impl: AppHistoryRepositoryImpl): AppHistoryRepository
}
