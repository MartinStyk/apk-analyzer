package sk.styk.martin.apkanalyzer.core.userpreferences.impl.recentlyviewed.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.userpreferences.api.recentlyviewed.RecentlyViewedAppsRepository
import sk.styk.martin.apkanalyzer.core.userpreferences.impl.db.UserPreferencesDatabase
import sk.styk.martin.apkanalyzer.core.userpreferences.impl.recentlyviewed.RecentlyViewedAppDao
import sk.styk.martin.apkanalyzer.core.userpreferences.impl.recentlyviewed.RecentlyViewedAppsRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface RecentlyViewedAppsModule {
    @Binds
    @Singleton
    fun bindRecentlyViewedAppsRepository(impl: RecentlyViewedAppsRepositoryImpl): RecentlyViewedAppsRepository

    companion object {
        @Provides
        @Singleton
        fun provideRecentlyViewedAppDao(database: UserPreferencesDatabase): RecentlyViewedAppDao = database.recentlyViewedAppDao()
    }
}
