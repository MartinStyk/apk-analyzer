package sk.styk.martin.apkanalyzer.core.userpreferences.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.userpreferences.RecentlyViewedAppsRepository
import sk.styk.martin.apkanalyzer.core.userpreferences.RecentlyViewedAppsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.userpreferences.SearchHistoryRepository
import sk.styk.martin.apkanalyzer.core.userpreferences.SearchHistoryRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface UserPreferencesModule {
    @Binds
    @Singleton
    fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository

    @Binds
    @Singleton
    fun bindRecentlyViewedAppsRepository(impl: RecentlyViewedAppsRepositoryImpl): RecentlyViewedAppsRepository
}
