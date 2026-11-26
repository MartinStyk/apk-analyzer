package sk.styk.martin.apkanalyzer.core.userpreferences.impl.searchhistory.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.userpreferences.api.searchhistory.SearchHistoryRepository
import sk.styk.martin.apkanalyzer.core.userpreferences.impl.db.UserPreferencesDatabase
import sk.styk.martin.apkanalyzer.core.userpreferences.impl.searchhistory.SearchHistoryDao
import sk.styk.martin.apkanalyzer.core.userpreferences.impl.searchhistory.SearchHistoryRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SearchHistoryModule {
    @Binds
    @Singleton
    fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository

    companion object {
        @Provides
        @Singleton
        fun provideSearchHistoryDao(database: UserPreferencesDatabase): SearchHistoryDao = database.searchHistoryDao()
    }
}
