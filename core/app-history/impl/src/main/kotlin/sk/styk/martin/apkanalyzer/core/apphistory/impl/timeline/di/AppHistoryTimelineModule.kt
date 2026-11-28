package sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline.di

import androidx.lifecycle.DefaultLifecycleObserver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline.AppHistoryPreloader
import sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline.AppHistoryRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppHistoryTimelineModule {
    @Binds
    @Singleton
    fun bindAppHistoryRepository(impl: AppHistoryRepositoryImpl): AppHistoryRepository

    @Binds
    @Singleton
    @IntoSet
    fun bindAppHistoryPreloader(impl: AppHistoryPreloader): DefaultLifecycleObserver
}
